'use strict';

const {
  sha256Hex,
  randomToken,
  publicFingerprint,
  safetyCode,
  turnCredentials,
  constantTimeEqual
} = require('./crypto');
const { verifyEnvelope } = require('./signing');
const { badRequest, forbidden, notFound, conflict, tooMany } = require('./errors');
const { checkRateLimit } = require('./db');

const CHATWAVE_ID = /^CW-[A-HJ-NP-Z2-9]{8}$/;
const HEX_64 = /^[0-9a-f]{64}$/;
const FINGERPRINT = /^([0-9A-F]{2}:){31}[0-9A-F]{2}$/;

function assertChatWaveId(value) {
  if (typeof value !== 'string' || !CHATWAVE_ID.test(value)) badRequest('invalid_chatwave_id');
  return value;
}

function assertPublicKey(value) {
  if (typeof value !== 'string' || value.length < 80 || value.length > 4096) badRequest('invalid_public_key');
  try {
    publicFingerprint(value);
  } catch {
    badRequest('invalid_public_key');
  }
  return value;
}

function assertHex64(value, code = 'invalid_hash') {
  if (typeof value !== 'string' || !HEX_64.test(value)) badRequest(code);
  return value;
}

function assertFingerprint(value) {
  if (typeof value !== 'string' || !FINGERPRINT.test(value)) badRequest('invalid_fingerprint');
  return value;
}

function publicUser(db, id) {
  const row = db.prepare(`
    SELECT chatwave_id, display_name, public_key, public_key_version, created_at, updated_at
    FROM users WHERE chatwave_id = ? AND revoked = 0
  `).get(assertChatWaveId(id));
  if (!row) notFound('user_not_found');
  return {
    chatwaveId: row.chatwave_id,
    displayName: row.display_name,
    publicKey: row.public_key,
    publicKeyVersion: row.public_key_version,
    fingerprint: publicFingerprint(row.public_key),
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

function safetyCodeFor(db, firstId, secondId) {
  const first = publicUser(db, firstId);
  const second = publicUser(db, secondId);
  return safetyCode(first.chatwaveId, first.fingerprint, second.chatwaveId, second.fingerprint);
}

function activeFingerprint(db, id) {
  const user = publicUser(db, id);
  return user.fingerprint;
}

function requireEnvelope(db, envelope, expectedType, publicKey, config, expectedFrom, expectedKeyVersion = null) {
  const result = verifyEnvelope(db, envelope, publicKey, expectedType, config);
  if (!result.ok) badRequest(result.reason);
  if (expectedFrom && envelope.fromId !== expectedFrom) forbidden('envelope_sender_mismatch');
  if (expectedKeyVersion != null && envelope.keyVersion !== expectedKeyVersion) forbidden('key_version_mismatch');
  return result.payload;
}

function registerUser(db, body, config) {
  const id = assertChatWaveId(body.chatwaveId);
  const publicKey = assertPublicKey(body.publicKey);
  const recoveryHash = assertHex64(body.recoveryHash, 'invalid_recovery_hash');
  const version = Number(body.publicKeyVersion || 1);
  if (!Number.isInteger(version) || version < 1) badRequest('invalid_public_key_version');
  if (typeof body.displayName !== 'string' || body.displayName.trim().length < 1 || body.displayName.length > 80) {
    badRequest('invalid_display_name');
  }
  const payload = requireEnvelope(db, body.envelope, 'user.register', publicKey, config, id, version);
  if (
    payload.chatwaveId !== id ||
    payload.publicKey !== publicKey ||
    payload.publicKeyVersion !== version ||
    payload.recoveryHash !== recoveryHash ||
    payload.displayName !== body.displayName.trim()
  ) {
    badRequest('envelope_payload_mismatch');
  }

  const existing = db.prepare('SELECT public_key FROM users WHERE chatwave_id = ?').get(id);
  if (existing) {
    if (existing.public_key !== publicKey) conflict('chatwave_id_taken');
    return publicUser(db, id);
  }

  const now = Date.now();
  db.transaction(() => {
    db.prepare(`
      INSERT INTO users (chatwave_id, display_name, public_key, public_key_version, recovery_hash, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(id, body.displayName.trim(), publicKey, version, recoveryHash, now, now);
    db.prepare(`
      INSERT INTO device_keys (chatwave_id, version, public_key, fingerprint, created_at, active)
      VALUES (?, ?, ?, ?, ?, 1)
    `).run(id, version, publicKey, publicFingerprint(publicKey), now);
    if (typeof body.deviceId === 'string' && body.deviceId.length <= 128) {
      db.prepare(`
        INSERT INTO devices (device_id, chatwave_id, display_name, created_at, last_seen)
        VALUES (?, ?, ?, ?, ?)
      `).run(body.deviceId, id, body.displayName.trim(), now, now);
    }
  })();
  return publicUser(db, id);
}

function recoverIdentity(db, body, config) {
  const id = assertChatWaveId(body.chatwaveId);
  const publicKey = assertPublicKey(body.publicKey);
  const recoveryToken = String(body.recoveryToken || '');
  if (recoveryToken.length < 24) badRequest('invalid_recovery_token');
  const version = Number(body.publicKeyVersion || 1);
  if (!Number.isInteger(version) || version < 1) badRequest('invalid_public_key_version');
  const payload = requireEnvelope(db, body.envelope, 'identity.recover', publicKey, config, id, version);
  if (payload.chatwaveId !== id || payload.publicKey !== publicKey || payload.publicKeyVersion !== version) {
    badRequest('envelope_payload_mismatch');
  }
  const row = db.prepare('SELECT recovery_hash, public_key_version FROM users WHERE chatwave_id = ?').get(id);
  if (!row) notFound('user_not_found');
  if (!constantTimeEqual(sha256Hex(recoveryToken), row.recovery_hash)) forbidden('invalid_recovery_token');
  if (version <= row.public_key_version) badRequest('public_key_version_not_increasing');
  const now = Date.now();
  db.transaction(() => {
    db.prepare('UPDATE device_keys SET active = 0 WHERE chatwave_id = ?').run(id);
    db.prepare(`
      INSERT INTO device_keys (chatwave_id, version, public_key, fingerprint, created_at, active)
      VALUES (?, ?, ?, ?, ?, 1)
    `).run(id, version, publicKey, publicFingerprint(publicKey), now);
    db.prepare(`
      UPDATE users SET public_key = ?, public_key_version = ?, updated_at = ? WHERE chatwave_id = ?
    `).run(publicKey, version, now, id);
  })();
  return publicUser(db, id);
}

function createInvitation(db, user, body, config) {
  const codeHash = assertHex64(body.codeHash, 'invalid_code_hash');
  const expiresAt = Number(body.expiresAt);
  const now = Date.now();
  const maxExpiry = now + (config.invitationTtlSeconds + 30) * 1000;
  if (!Number.isInteger(expiresAt) || expiresAt <= now || expiresAt > maxExpiry) badRequest('invalid_expiration');
  const payload = requireEnvelope(db, body.envelope, 'invitation.create', user.publicKey, config, user.chatwaveId, user.publicKeyVersion);
  if (payload.codeHash !== codeHash || Number(payload.expiresAt) !== expiresAt) badRequest('envelope_payload_mismatch');
  if (!checkRateLimit(db, `invite:${user.chatwaveId}`, config.rateLimitMax, config.rateLimitWindowSeconds, now)) {
    tooMany();
  }
  try {
    db.prepare(`
      INSERT INTO invitations (code_hash, inviter_id, expires_at, created_at)
      VALUES (?, ?, ?, ?)
    `).run(codeHash, user.chatwaveId, expiresAt, now);
  } catch (error) {
    if (String(error.code || '').startsWith('SQLITE_CONSTRAINT')) conflict('invitation_exists');
    throw error;
  }
  return { codeHash, expiresAt };
}

function redeemInvitation(db, user, body, config) {
  const code = String(body.code || '').replace(/[^0-9]/g, '');
  if (code.length !== 9) badRequest('invalid_invitation_code');
  const codeHash = sha256Hex(code);
  const payload = requireEnvelope(db, body.envelope, 'invitation.redeem', user.publicKey, config, user.chatwaveId, user.publicKeyVersion);
  if (payload.codeHash !== codeHash || payload.chatwaveId !== user.chatwaveId) badRequest('envelope_payload_mismatch');
  const now = Date.now();
  const outcome = db.transaction(() => {
    const row = db.prepare('SELECT * FROM invitations WHERE code_hash = ?').get(codeHash);
    if (!row) notFound('invitation_not_found');
    if (row.used_at || row.canceled_at) conflict('invitation_unavailable');
    if (row.expires_at < now) conflict('invitation_expired');
    if (row.inviter_id === user.chatwaveId) forbidden('cannot_redeem_own_invitation');
    if (row.attempts >= 5) {
      db.prepare('UPDATE invitations SET canceled_at = ? WHERE code_hash = ? AND used_at IS NULL').run(now, codeHash);
      tooMany('invitation_attempts_exceeded');
    }
    db.prepare('UPDATE invitations SET attempts = attempts + 1 WHERE code_hash = ?').run(codeHash);
    db.prepare('UPDATE invitations SET used_at = ?, redeemed_by = ? WHERE code_hash = ? AND used_at IS NULL AND canceled_at IS NULL').run(now, user.chatwaveId, codeHash);
    const updated = db.prepare('SELECT * FROM invitations WHERE code_hash = ?').get(codeHash);
    if (updated.used_at !== now || updated.redeemed_by !== user.chatwaveId) conflict('invitation_unavailable');
    return updated;
  })();
  const inviter = publicUser(db, outcome.inviter_id);
  const codeValue = safetyCode(user.chatwaveId, activeFingerprint(db, user.chatwaveId), inviter.chatwaveId, inviter.fingerprint);
  db.prepare(`
    INSERT INTO trusted_contacts (owner_id, contact_id, verified, safety_code, key_version, created_at, updated_at)
    VALUES (?, ?, 0, ?, ?, ?, ?)
    ON CONFLICT(owner_id, contact_id) DO UPDATE SET
      safety_code = excluded.safety_code, key_version = excluded.key_version, updated_at = excluded.updated_at
  `).run(user.chatwaveId, inviter.chatwaveId, codeValue, inviter.publicKeyVersion, now, now);
  return { inviter, safetyCode: codeValue };
}

function cancelInvitation(db, user, body, config) {
  const codeHash = assertHex64(body.codeHash, 'invalid_code_hash');
  const payload = requireEnvelope(db, body.envelope, 'invitation.cancel', user.publicKey, config, user.chatwaveId, user.publicKeyVersion);
  if (payload.codeHash !== codeHash) badRequest('envelope_payload_mismatch');
  const row = db.prepare('SELECT inviter_id, canceled_at, used_at FROM invitations WHERE code_hash = ?').get(codeHash);
  if (!row) notFound('invitation_not_found');
  if (row.inviter_id !== user.chatwaveId) forbidden('not_invitation_owner');
  if (row.used_at) conflict('invitation_unavailable');
  if (row.canceled_at) return { canceled: true };
  const result = db.prepare('UPDATE invitations SET canceled_at = ? WHERE code_hash = ? AND canceled_at IS NULL AND used_at IS NULL').run(Date.now(), codeHash);
  return { canceled: result.changes > 0 };
}

function callSession(db, user, id) {
  assertChatWaveId(user.chatwaveId);
  const row = db.prepare(`
    SELECT s.* FROM call_sessions s
    JOIN call_participants p ON p.session_id = s.id
    WHERE s.id = ? AND p.chatwave_id = ?
  `).get(id, user.chatwaveId);
  if (!row) notFound('call_not_found');
  const participants = db.prepare(`
    SELECT chatwave_id, role, fingerprint, ephemeral_public_key, joined_at
    FROM call_participants WHERE session_id = ?
  `).all(id);
  return {
    id: row.id,
    state: row.state,
    safetyBinding: row.safety_binding,
    relayOnly: Boolean(row.relay_only),
    createdAt: row.created_at,
    expiresAt: row.expires_at,
    endedAt: row.ended_at,
    participants
  };
}

async function createCall(db, user, body, config) {
  const calleeId = assertChatWaveId(body.calleeId);
  if (calleeId === user.chatwaveId) badRequest('cannot_call_self');
  const payload = requireEnvelope(db, body.envelope, 'call.create', user.publicKey, config, user.chatwaveId, user.publicKeyVersion);
  if (payload.calleeId !== calleeId) badRequest('envelope_payload_mismatch');
  const ephemeralPublicKey = assertPublicKey(payload.ephemeralPublicKey);
  const callerFingerprint = assertFingerprint(payload.fingerprint);
  const expectedCallerFingerprint = activeFingerprint(db, user.chatwaveId);
  if (callerFingerprint !== expectedCallerFingerprint) badRequest('fingerprint_mismatch');
  if (typeof payload.safetyBinding !== 'string' || !HEX_64.test(payload.safetyBinding)) badRequest('invalid_safety_binding');
  publicUser(db, calleeId);
  if (!checkRateLimit(db, `call:${user.chatwaveId}`, config.rateLimitMax, config.rateLimitWindowSeconds)) tooMany();
  const now = Date.now();
  const id = randomToken(16);
  const relayOnly = payload.relayOnly === true ? 1 : 0;
  db.transaction(() => {
    db.prepare(`
      INSERT INTO call_sessions (id, caller_id, callee_id, state, safety_binding, created_at, expires_at, relay_only)
      VALUES (?, ?, ?, 'ringing', ?, ?, ?, ?)
    `).run(id, user.chatwaveId, calleeId, payload.safetyBinding, now, now + config.callSessionTtlSeconds * 1000, relayOnly);
    db.prepare(`
      INSERT INTO call_participants (session_id, chatwave_id, role, fingerprint, ephemeral_public_key, joined_at)
      VALUES (?, ?, 'caller', ?, ?, ?)
    `).run(id, user.chatwaveId, callerFingerprint, ephemeralPublicKey, now);
    db.prepare(`
      INSERT INTO call_participants (session_id, chatwave_id, role, fingerprint, ephemeral_public_key, joined_at)
      VALUES (?, ?, 'callee', '', '', ?)
    `).run(id, calleeId, now);
  })();
  return callSession(db, user, id);
}

function acceptCall(db, user, id, body, config) {
  const session = callSession(db, user, id);
  if (session.state !== 'ringing') conflict('call_not_ringing');
  const participant = session.participants.find((item) => item.chatwave_id === user.chatwaveId);
  if (!participant || participant.role !== 'callee') forbidden('only_callee_can_accept');
  const payload = requireEnvelope(db, body.envelope, 'call.accept', user.publicKey, config, user.chatwaveId, user.publicKeyVersion);
  if (payload.sessionId !== id || payload.safetyBinding !== session.safetyBinding) badRequest('envelope_payload_mismatch');
  const ephemeralPublicKey = assertPublicKey(payload.ephemeralPublicKey);
  const calleeFingerprint = assertFingerprint(payload.fingerprint);
  if (calleeFingerprint !== activeFingerprint(db, user.chatwaveId)) badRequest('fingerprint_mismatch');
  db.transaction(() => {
    db.prepare(`
      UPDATE call_participants SET fingerprint = ?, ephemeral_public_key = ?, joined_at = ?
      WHERE session_id = ? AND chatwave_id = ?
    `).run(calleeFingerprint, ephemeralPublicKey, Date.now(), id, user.chatwaveId);
    db.prepare("UPDATE call_sessions SET state = 'connecting' WHERE id = ? AND state = 'ringing'").run(id);
  })();
  return callSession(db, user, id);
}

function updateCallState(db, user, id, body, config, expectedType, nextState) {
  const session = callSession(db, user, id);
  const payload = requireEnvelope(db, body.envelope, expectedType, user.publicKey, config, user.chatwaveId, user.publicKeyVersion);
  if (payload.sessionId !== id) badRequest('envelope_payload_mismatch');
  if (expectedType === 'call.decline') {
    const participant = session.participants.find((item) => item.chatwave_id === user.chatwaveId);
    if (!participant || participant.role !== 'callee') forbidden('only_callee_can_decline');
    if (session.state !== 'ringing') conflict('call_not_ringing');
  }
  if (expectedType === 'call.end' && ['ended', 'declined', 'missed', 'failed'].includes(session.state)) {
    return session;
  }
  db.prepare('UPDATE call_sessions SET state = ?, ended_at = COALESCE(ended_at, ?) WHERE id = ?').run(nextState, Date.now(), id);
  return callSession(db, user, id);
}

function signal(db, user, id, body, config) {
  const session = callSession(db, user, id);
  if (!['connecting', 'active'].includes(session.state)) conflict('call_not_active');
  const payload = requireEnvelope(db, body.envelope, 'call.signal', user.publicKey, config, user.chatwaveId, user.publicKeyVersion);
  if (payload.sessionId !== id || !['offer', 'answer', 'ice', 'restart', 'active', 'end'].includes(payload.kind)) {
    badRequest('envelope_payload_mismatch');
  }
  const recipient = session.participants.find((item) => item.chatwave_id !== user.chatwaveId);
  if (!recipient) notFound('call_peer_not_found');
  db.prepare(`
    INSERT INTO call_events (session_id, type, sender_id, payload, nonce, created_at)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(id, payload.kind, user.chatwaveId, JSON.stringify(payload.data || {}), body.envelope.nonce, Date.now());
  if (payload.kind === 'active') db.prepare("UPDATE call_sessions SET state = 'active' WHERE id = ? AND state = 'connecting'").run(id);
  if (payload.kind === 'end') db.prepare("UPDATE call_sessions SET state = 'ended', ended_at = COALESCE(ended_at, ?) WHERE id = ?",).run(Date.now(), id);
  return { recipient: recipient.chatwave_id, kind: payload.kind, data: payload.data || {}, nonce: body.envelope.nonce };
}

function turnCredentialsFor(db, user, body, config) {
  const payload = requireEnvelope(db, body.envelope, 'turn.credentials', user.publicKey, config, user.chatwaveId, user.publicKeyVersion);
  if (payload.chatwaveId !== user.chatwaveId) badRequest('envelope_payload_mismatch');
  if (!config.turnSecret) badRequest('turn_not_configured');
  return { ...turnCredentials(config.turnSecret, user.chatwaveId, config.turnTtlSeconds), urls: config.turnUrls };
}

function registerPush(db, user, body, config) {
  const endpoint = String(body.endpoint || '');
  let parsed;
  try {
    parsed = new URL(endpoint);
  } catch {
    badRequest('invalid_endpoint');
  }
  if (!['http:', 'https:'].includes(parsed.protocol)) badRequest('invalid_endpoint');
  if (parsed.protocol === 'http:' && !['localhost', '127.0.0.1', '10.0.2.2'].includes(parsed.hostname)) {
    badRequest('insecure_endpoint');
  }
  const endpointHash = sha256Hex(endpoint);
  const payload = requireEnvelope(db, body.envelope, 'push.register', user.publicKey, config, user.chatwaveId, user.publicKeyVersion);
  if (payload.endpointHash !== endpointHash) badRequest('envelope_payload_mismatch');
  const now = Date.now();
  db.prepare(`
    INSERT INTO push_devices (endpoint_hash, chatwave_id, endpoint, created_at, last_seen)
    VALUES (?, ?, ?, ?, ?)
    ON CONFLICT(endpoint_hash) DO UPDATE SET chatwave_id = excluded.chatwave_id, last_seen = excluded.last_seen
  `).run(endpointHash, user.chatwaveId, endpoint, now, now);
  return { registered: true };
}

async function notifyUser(db, config, userId, message) {
  const rows = db.prepare('SELECT endpoint FROM push_devices WHERE chatwave_id = ?').all(userId);
  await Promise.all(rows.map(async (row) => {
    try {
      const response = await fetch(row.endpoint, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ chatwave: message }),
        signal: AbortSignal.timeout(config.unifiedPushTimeoutMs)
      });
      if (response.status === 404 || response.status === 410) {
        db.prepare('DELETE FROM push_devices WHERE endpoint = ?').run(row.endpoint);
      }
    } catch {
      // Keep endpoint on transient errors; UnifiedPush gateways can be temporarily unavailable.
    }
  }));
}

function updateDevicePresence(db, user) {
  if (!user.deviceId) return;
  db.prepare(`
    UPDATE devices SET last_seen = ? WHERE device_id = ? AND chatwave_id = ?
  `).run(Date.now(), user.deviceId, user.chatwaveId);
}

module.exports = {
  publicUser,
  registerUser,
  recoverIdentity,
  createInvitation,
  redeemInvitation,
  cancelInvitation,
  lookupUser: publicUser,
  createCall,
  acceptCall,
  updateCallState,
  signal,
  callSession,
  turnCredentialsFor,
  registerPush,
  notifyUser,
  updateDevicePresence,
  activeFingerprint,
  safetyCodeFor
};
