'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { sha256Hex, publicFingerprint } = require('../src/crypto');
const { startTestServer, request, keyPair, envelope, registerUser } = require('./helpers');

async function seedPair(server) {
  const aliceKeys = keyPair();
  const bobKeys = keyPair();
  const carolKeys = keyPair();
  const alice = await registerUser(server, 'CW-CCCC4444', 'Alice', aliceKeys.privateKey, aliceKeys.publicKeyDer);
  const bob = await registerUser(server, 'CW-DDDD5555', 'Bob', bobKeys.privateKey, bobKeys.publicKeyDer);
  const carol = await registerUser(server, 'CW-EEEE6666', 'Carol', carolKeys.privateKey, carolKeys.publicKeyDer);
  return { alice, bob, carol };
}

test('call creation, participant authorization, signaling, replay protection, and termination', async () => {
  const server = await startTestServer();
  try {
    const { alice, bob, carol } = await seedPair(server);
    const aliceEph = keyPair();
    const bobEph = keyPair();
    const safetyBinding = sha256Hex('session-binding-value');

    const createPayload = {
      calleeId: bob.chatwaveId,
      ephemeralPublicKey: aliceEph.publicKeyDer,
      fingerprint: alice.fingerprint,
      safetyBinding,
      relayOnly: true
    };
    const created = await request(server, 'POST', '/api/calls', {
      calleeId: bob.chatwaveId,
      envelope: envelope('call.create', alice.chatwaveId, createPayload, alice.privateKey)
    });
    assert.equal(created.status, 201);
    assert.equal(created.body.state, 'ringing');
    assert.equal(created.body.relayOnly, true);
    const sessionId = created.body.id;

    const unauthorizedRead = await request(server, 'POST', `/api/calls/${sessionId}/status`, {
      envelope: envelope('call.status', carol.chatwaveId, { sessionId }, carol.privateKey, { sessionId })
    }, 'device-carol');
    assert.equal(unauthorizedRead.status, 404);
    assert.equal(unauthorizedRead.body.code, 'call_not_found');

    const acceptPayload = {
      sessionId,
      ephemeralPublicKey: bobEph.publicKeyDer,
      fingerprint: publicFingerprint(bob.publicKeyDer),
      safetyBinding
    };
    const accepted = await request(server, 'POST', `/api/calls/${sessionId}/accept`, {
      envelope: envelope('call.accept', bob.chatwaveId, acceptPayload, bob.privateKey, { sessionId })
    });
    assert.equal(accepted.status, 200);
    assert.equal(accepted.body.state, 'connecting');

    const signalPayload = { sessionId, kind: 'offer', data: { sdp: 'v=0 ...', fingerprint: alice.fingerprint } };
    const signalEnvelope = envelope('call.signal', alice.chatwaveId, signalPayload, alice.privateKey, { sessionId });
    const signal = await request(server, 'POST', `/api/calls/${sessionId}/signal`, { envelope: signalEnvelope });
    assert.equal(signal.status, 200);
    assert.equal(signal.body.sent, true);

    const replay = await request(server, 'POST', `/api/calls/${sessionId}/signal`, { envelope: signalEnvelope });
    assert.equal(replay.status, 400);
    assert.equal(replay.body.code, 'replayed_nonce');

    const carolSignal = await request(server, 'POST', `/api/calls/${sessionId}/signal`, {
      envelope: envelope('call.signal', carol.chatwaveId, signalPayload, carol.privateKey, { sessionId })
    });
    assert.equal(carolSignal.status, 404);

    const ended = await request(server, 'POST', `/api/calls/${sessionId}/end`, {
      envelope: envelope('call.end', bob.chatwaveId, { sessionId }, bob.privateKey, { sessionId })
    });
    assert.equal(ended.status, 200);
    assert.equal(ended.body.state, 'ended');
  } finally {
    await server.close();
  }
});

test('TURN credentials are short-lived and bound to a signed identity request', async () => {
  const server = await startTestServer({ turnSecret: 'test-turn-secret', turnTtlSeconds: 600 });
  try {
    const keys = keyPair();
    const user = await registerUser(server, 'CW-FFFF7777', 'Turn User', keys.privateKey, keys.publicKeyDer);
    const payload = { chatwaveId: user.chatwaveId };
    const response = await request(server, 'POST', '/api/turn/credentials', {
      envelope: envelope('turn.credentials', user.chatwaveId, payload, keys.privateKey)
    });
    assert.equal(response.status, 200);
    assert.ok(response.body.username.startsWith(''));
    assert.ok(response.body.username.includes(user.chatwaveId));
    assert.ok(response.body.credential.length > 10);
    assert.ok(response.body.expires <= Math.floor(Date.now() / 1000) + 600);
  } finally {
    await server.close();
  }
});
