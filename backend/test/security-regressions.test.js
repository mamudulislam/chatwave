'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const { startTestServer, request, envelope, keyPair, waitForMessage } = require('./helpers');
const WebSocket = require('ws');
const { sha256Hex, createSignature, publicFingerprint } = require('../src/crypto');

test('websocket signal accepts nested envelope', async (t) => {
  const server = await startTestServer();
  t.after(() => server.close());
  const aliceKeys = keyPair();
  const bobKeys = keyPair();
  const fp = publicFingerprint(aliceKeys.publicKeyDer);
  await server.db.prepare(`INSERT INTO users (chatwave_id, display_name, public_key, public_key_version, recovery_hash, created_at, updated_at) VALUES (?, ?, ?, 1, ?, ?, ?)`).run('CW-AAAA3333', 'Alice', aliceKeys.publicKeyDer, sha256Hex('r1-aaaaaaaaaaaaaaaaaaaaaaaa'), Date.now(), Date.now());
  await server.db.prepare(`INSERT INTO users (chatwave_id, display_name, public_key, public_key_version, recovery_hash, created_at, updated_at) VALUES (?, ?, ?, 1, ?, ?, ?)`).run('CW-BBBB4444', 'Bob', bobKeys.publicKeyDer, sha256Hex('r2-bbbbbbbbbbbbbbbbbbbbbbbb'), Date.now(), Date.now());
  const create = await request(server, 'POST', '/api/calls', { calleeId: 'CW-BBBB4444', envelope: envelope('call.create', 'CW-AAAA3333', { calleeId: 'CW-BBBB4444', ephemeralPublicKey: aliceKeys.publicKeyDer, fingerprint: fp, safetyBinding: '1'.repeat(64), relayOnly: false }, aliceKeys.privateKey) });
  assert.equal(create.status, 201);
  const sessionId = create.body.id;
  // Move to connecting so signal is allowed
  await server.db.prepare("UPDATE call_sessions SET state='connecting' WHERE id=?").run(sessionId);
  const ws = new WebSocket(server.wsUrl);
  t.after(() => { try { ws.terminate(); } catch {} });
  const challenge = await waitForMessage(ws, (m) => m.type === 'auth_challenge');
  const authUnsigned = { type: 'auth', chatwaveId: 'CW-AAAA3333', publicKeyVersion: 1, deviceId: 'a', nonce: challenge.nonce };
  ws.send(JSON.stringify({ ...authUnsigned, signature: createSignature(authUnsigned, aliceKeys.privateKey) }));
  await waitForMessage(ws, (m) => m.type === 'auth_ok');
  const sigEnv = envelope('call.signal', 'CW-AAAA3333', { sessionId, kind: 'ice', data: { candidate: 'x' } }, aliceKeys.privateKey, { sessionId });
  ws.send(JSON.stringify({ type: 'signal', sessionId, envelope: sigEnv }));
  const msg = await waitForMessage(ws, (m) => m.type === 'error' || m.type === 'signal_ack');
  assert.equal(msg.type, 'signal_ack', JSON.stringify(msg));
});
