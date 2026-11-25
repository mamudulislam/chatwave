'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { WebSocket } = require('ws');
const { canonicalJson, sha256Hex, publicFingerprint } = require('../src/crypto');
const { startTestServer, request, keyPair, envelope, registerUser, waitForMessage } = require('./helpers');

test('WebSocket requires signed identity and relays authorized incoming calls', async () => {
  const server = await startTestServer();
  const socket = new WebSocket(server.wsUrl);
  try {
    const challenge = await waitForMessage(socket, (message) => message.type === 'auth_challenge');
    const bobKeys = keyPair();
    const bob = await registerUser(server, 'CW-GGGG8888', 'Bob', bobKeys.privateKey, bobKeys.publicKeyDer);
    const unsigned = {
      type: 'auth',
      chatwaveId: bob.chatwaveId,
      publicKeyVersion: 1,
      deviceId: 'device-bob',
      nonce: challenge.nonce
    };
    const signature = crypto.sign('sha256', Buffer.from(canonicalJson(unsigned)), bobKeys.privateKey).toString('base64');
    socket.send(JSON.stringify({ ...unsigned, type: 'auth', signature }));
    const authOk = await waitForMessage(socket, (message) => message.type === 'auth_ok');
    assert.equal(authOk.chatwaveId, bob.chatwaveId);
    assert.equal(authOk.fingerprint, publicFingerprint(bob.publicKeyDer));

    const aliceKeys = keyPair();
    const alice = await registerUser(server, 'CW-HHHH9999', 'Alice', aliceKeys.privateKey, aliceKeys.publicKeyDer);
    const aliceEph = keyPair();
    const safetyBinding = sha256Hex('ws-binding');
    const incomingPromise = waitForMessage(socket, (message) => message.type === 'incoming_call');
    const created = await request(server, 'POST', '/api/calls', {
      calleeId: bob.chatwaveId,
      envelope: envelope('call.create', alice.chatwaveId, {
        calleeId: bob.chatwaveId,
        ephemeralPublicKey: aliceEph.publicKeyDer,
        fingerprint: alice.fingerprint,
        safetyBinding,
        relayOnly: false
      }, aliceKeys.privateKey)
    });
    assert.equal(created.status, 201);
    const incoming = await incomingPromise;
    assert.equal(incoming.sessionId, created.body.id);
    assert.equal(incoming.caller.chatwaveId, alice.chatwaveId);
  } finally {
    if (socket.readyState === WebSocket.OPEN) socket.close();
    await server.close();
  }
});
