'use strict';

const { canonicalJson, verifySignature, createSignature } = require('./crypto');
const { consumeNonce } = require('./db');

function unsignedEnvelope(envelope) {
  return {
    type: envelope.type,
    fromId: envelope.fromId,
    toId: envelope.toId ?? null,
    sessionId: envelope.sessionId ?? null,
    payload: envelope.payload,
    timestamp: envelope.timestamp,
    nonce: envelope.nonce,
    keyVersion: envelope.keyVersion
  };
}

function envelopePayload(envelope) {
  try {
    return JSON.parse(envelope.payload);
  } catch {
    return null;
  }
}

function validateEnvelopeShape(envelope, expectedType) {
  if (!envelope || typeof envelope !== 'object') return false;
  if (expectedType && envelope.type !== expectedType) return false;
  if (typeof envelope.fromId !== 'string' || !envelope.fromId) return false;
  if (typeof envelope.payload !== 'string' || !envelope.payload) return false;
  if (!Number.isInteger(envelope.timestamp)) return false;
  if (typeof envelope.nonce !== 'string' || envelope.nonce.length < 16) return false;
  if (!Number.isInteger(envelope.keyVersion) || envelope.keyVersion < 1) return false;
  if (typeof envelope.signature !== 'string' || !envelope.signature) return false;
  return envelopePayload(envelope) !== null;
}

function verifyEnvelope(db, envelope, expectedPublicKey, expectedType, config, now = Date.now()) {
  if (!validateEnvelopeShape(envelope, expectedType)) return { ok: false, reason: 'invalid_envelope' };
  if (Math.abs(now - envelope.timestamp) > config.signingMaxAgeSeconds * 1000) {
    return { ok: false, reason: 'stale_envelope' };
  }
  if (!verifySignature(unsignedEnvelope(envelope), envelope.signature, expectedPublicKey)) {
    return { ok: false, reason: 'invalid_signature' };
  }
  if (!consumeNonce(db, envelope.nonce, `envelope:${envelope.type}`, now)) {
    return { ok: false, reason: 'replayed_nonce' };
  }
  return { ok: true, payload: envelopePayload(envelope) };
}

function createEnvelope(unsigned, privateKey, signature) {
  const computed = signature || createSignature(unsigned, privateKey);
  return canonicalJson({ ...unsigned, signature: computed });
}

module.exports = {
  unsignedEnvelope,
  envelopePayload,
  validateEnvelopeShape,
  verifyEnvelope,
  createEnvelope
};
