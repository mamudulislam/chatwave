'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { canonicalJson, safetyCode, publicFingerprint, sha256Hex } = require('../src/crypto');
const { startTestServer, request, keyPair, envelope, registerUser } = require('./helpers');

test('canonical JSON is stable across key order and nested values', () => {
  assert.equal(
    canonicalJson({ b: 2, a: { d: 'x', c: [true, null, 3] } }),
    '{"a":{"c":[true,null,3],"d":"x"},"b":2}'
  );
});

test('safety code is pair-order independent', () => {
  const first = safetyCode('CW-AAAAAAAA', 'AA:BB', 'CW-BBBBBBBB', 'CC:DD');
  const second = safetyCode('CW-BBBBBBBB', 'CC:DD', 'CW-AAAAAAAA', 'AA:BB');
  assert.equal(first, second);
  assert.match(first, /^[A-HJ-NP-Z2-9]{4}( [A-HJ-NP-Z2-9]{4}){4}$/);
});

test('registration, discovery, and recovery rotate public keys', async () => {
  const server = await startTestServer();
  try {
    const first = keyPair();
    const user = await registerUser(server, 'CW-8F29K7X4', 'Alice', first.privateKey, first.publicKeyDer);
    assert.equal(user.chatwaveId, 'CW-8F29K7X4');
    assert.equal(user.publicKeyVersion, 1);

    const lookup = await request(server, 'GET', '/api/users/CW-8F29K7X4', undefined, 'device-viewer');
    assert.equal(lookup.status, 200);
    assert.equal(lookup.body.fingerprint, publicFingerprint(first.publicKeyDer));

    const next = keyPair();
    const payload = {
      chatwaveId: user.chatwaveId,
      publicKey: next.publicKeyDer,
      publicKeyVersion: 2
    };
    const recovery = await request(server, 'POST', '/api/identity/recover', {
      ...payload,
      recoveryToken: user.recoveryToken,
      envelope: envelope('identity.recover', user.chatwaveId, payload, next.privateKey, { keyVersion: 2 })
    });
    assert.equal(recovery.status, 200);
    assert.equal(recovery.body.publicKeyVersion, 2);
    assert.equal(recovery.body.publicKey, next.publicKeyDer);

    const badRecovery = await request(server, 'POST', '/api/identity/recover', {
      ...payload,
      recoveryToken: 'wrong-token-that-is-long-enough',
      envelope: envelope('identity.recover', user.chatwaveId, payload, next.privateKey, {
        keyVersion: 3,
        timestamp: Date.now(),
        nonce: crypto.randomBytes(18).toString('base64url')
      })
    });
    assert.equal(badRecovery.status, 403);

    const hash = sha256Hex('abc');
    assert.equal(hash.length, 64);
  } finally {
    await server.close();
  }
});
