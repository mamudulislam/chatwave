'use strict';

const crypto = require('node:crypto');
const { webcrypto } = require('node:crypto');
const { WebSocket } = require('ws');
const { createApp } = require('../src/server');
const config = require('../src/config');
const { openDatabase } = require('../src/db');
const { sha256Hex } = require('../src/crypto');

async function startTestServer(overrides = {}) {
  const instance = createApp({ db: openDatabase(':memory:', config.migrationDir), config: overrides });
  await new Promise((resolve) => instance.server.listen(0, '127.0.0.1', resolve));
  const address = instance.server.address();
  return {
    ...instance,
    baseUrl: `http://127.0.0.1:${address.port}`,
    wsUrl: `ws://127.0.0.1:${address.port}/ws`,
    close: async () => {
      for (const client of instance.context.realtime.wss.clients) client.terminate();
      await new Promise((resolve) => instance.server.close(resolve));
      instance.db.close();
    }
  };
}

async function request(server, method, path, body, deviceId) {
  const response = await fetch(`${server.baseUrl}${path}`, {
    method,
    headers: {
      'content-type': 'application/json',
      ...(deviceId ? { 'x-chatwave-device': deviceId } : {})
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : null };
}

function keyPair() {
  const { privateKey, publicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
  return {
    privateKey,
    publicKeyDer: publicKey.export({ type: 'spki', format: 'der' }).toString('base64')
  };
}

function envelope(type, fromId, payload, privateKey, options = {}) {
  const unsigned = {
    type,
    fromId,
    toId: options.toId ?? null,
    sessionId: options.sessionId ?? null,
    payload: typeof payload === 'string' ? payload : JSON.stringify(payload),
    timestamp: options.timestamp ?? Date.now(),
    nonce: options.nonce ?? crypto.randomBytes(18).toString('base64url'),
    keyVersion: options.keyVersion ?? 1
  };
  const canonical = require('../src/crypto').canonicalJson(unsigned);
  const signature = crypto.sign('sha256', Buffer.from(canonical), privateKey).toString('base64');
  return { ...unsigned, signature };
}

async function registerUser(server, id, displayName, privateKey, publicKeyDer) {
  const recoveryToken = `RECOVERY-${id}-${crypto.randomBytes(24).toString('base64url')}`;
  const recoveryHash = sha256Hex(recoveryToken);
  const payload = { chatwaveId: id, displayName, publicKey: publicKeyDer, publicKeyVersion: 1, recoveryHash };
  const body = {
    ...payload,
    deviceId: `device-${id}`,
    envelope: envelope('user.register', id, payload, privateKey)
  };
  const response = await request(server, 'POST', '/api/users/register', body);
  if (response.status !== 200) throw new Error(`register failed: ${JSON.stringify(response)}`);
  return { ...response.body, recoveryToken, privateKey, publicKeyDer };
}

function waitForMessage(socket, predicate, timeoutMs = 3000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      socket.off('message', onMessage);
      reject(new Error('timeout waiting for websocket message'));
    }, timeoutMs);
    const onMessage = (raw) => {
      const message = JSON.parse(raw.toString());
      if (!predicate(message)) return;
      clearTimeout(timer);
      socket.off('message', onMessage);
      resolve(message);
    };
    socket.on('message', onMessage);
  });
}

module.exports = { startTestServer, request, keyPair, envelope, registerUser, waitForMessage, webcrypto };
