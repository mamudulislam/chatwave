'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { sha256Hex } = require('../src/crypto');
const { startTestServer, request, keyPair, envelope, registerUser } = require('./helpers');

async function seedPair(server) {
  const aliceKeys = keyPair();
  const bobKeys = keyPair();
  const alice = await registerUser(server, 'CW-AAAA2222', 'Alice', aliceKeys.privateKey, aliceKeys.publicKeyDer);
  const bob = await registerUser(server, 'CW-BBBB3333', 'Bob', bobKeys.privateKey, bobKeys.publicKeyDer);
  return { alice, bob };
}

test('invitation codes are single-use and bound to the redeemer', async () => {
  const server = await startTestServer();
  try {
    const { alice, bob } = await seedPair(server);
    const code = '729418635';
    const codeHash = sha256Hex(code);
    const expiresAt = Date.now() + 300000;
    const create = await request(server, 'POST', '/api/invitations', {
      codeHash,
      expiresAt,
      envelope: envelope('invitation.create', alice.chatwaveId, { codeHash, expiresAt }, alice.privateKey, {
        deviceId: 'device-CW-AAAA2222'
      })
    }, 'device-CW-AAAA2222');
    assert.equal(create.status, 201);

    const redeemPayload = { codeHash, chatwaveId: bob.chatwaveId };
    const redeemBody = {
      code,
      envelope: envelope('invitation.redeem', bob.chatwaveId, redeemPayload, bob.privateKey)
    };
    const first = await request(server, 'POST', '/api/invitations/redeem', redeemBody, 'device-CW-BBBB3333');
    assert.equal(first.status, 200);
    assert.equal(first.body.inviter.chatwaveId, alice.chatwaveId);
    assert.match(first.body.safetyCode, /^[A-HJ-NP-Z2-9]{4}( [A-HJ-NP-Z2-9]{4}){4}$/);

    const replay = await request(server, 'POST', '/api/invitations/redeem', {
      code,
      envelope: envelope('invitation.redeem', bob.chatwaveId, redeemPayload, bob.privateKey)
    }, 'device-CW-BBBB3333');
    assert.equal(replay.status, 409);
    assert.equal(replay.body.code, 'invitation_unavailable');
  } finally {
    await server.close();
  }
});

test('invitations expire and can be canceled by the inviter only', async () => {
  const server = await startTestServer();
  try {
    const { alice, bob } = await seedPair(server);
    const shortCode = '111222333';
    const shortHash = sha256Hex(shortCode);
    const expiresAt = Date.now() + 900;
    await request(server, 'POST', '/api/invitations', {
      codeHash: shortHash,
      expiresAt,
      envelope: envelope('invitation.create', alice.chatwaveId, { codeHash: shortHash, expiresAt }, alice.privateKey)
    });
    await new Promise((resolve) => setTimeout(resolve, 1100));
    const expired = await request(server, 'POST', '/api/invitations/redeem', {
      code: shortCode,
      envelope: envelope('invitation.redeem', bob.chatwaveId, {
        codeHash: shortHash,
        chatwaveId: bob.chatwaveId
      }, bob.privateKey)
    });
    assert.equal(expired.status, 409);
    assert.equal(expired.body.code, 'invitation_expired');

    const code = '444555666';
    const codeHash = sha256Hex(code);
    const cancelExpiresAt = Date.now() + 300000;
    await request(server, 'POST', '/api/invitations', {
      codeHash,
      expiresAt: cancelExpiresAt,
      envelope: envelope('invitation.create', alice.chatwaveId, { codeHash, expiresAt: cancelExpiresAt }, alice.privateKey)
    });
    const cancelByBob = await request(server, 'POST', '/api/invitations/cancel', {
      codeHash,
      envelope: envelope('invitation.cancel', bob.chatwaveId, { codeHash }, bob.privateKey)
    });
    assert.equal(cancelByBob.status, 403);

    const cancelByAlice = await request(server, 'POST', '/api/invitations/cancel', {
      codeHash,
      envelope: envelope('invitation.cancel', alice.chatwaveId, { codeHash }, alice.privateKey)
    });
    assert.equal(cancelByAlice.status, 200);
    const redeem = await request(server, 'POST', '/api/invitations/redeem', {
      code,
      envelope: envelope('invitation.redeem', bob.chatwaveId, { codeHash, chatwaveId: bob.chatwaveId }, bob.privateKey)
    });
    assert.equal(redeem.status, 409);
  } finally {
    await server.close();
  }
});
