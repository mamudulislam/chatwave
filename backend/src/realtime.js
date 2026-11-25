'use strict';

const { WebSocketServer } = require('ws');
const { randomToken, verifySignature, publicFingerprint } = require('./crypto');
const { consumeNonce } = require('./db');
const services = require('./services');

function attachRealtime(server, context) {
  const { db, config } = context;
  const wss = new WebSocketServer({ server, path: '/ws' });
  const socketsByUser = new Map();

  const send = (socket, message) => {
    if (socket.readyState === 1) socket.send(JSON.stringify(message));
  };

  const sendToUser = (userId, message) => {
    const sockets = socketsByUser.get(userId);
    if (!sockets || sockets.size === 0) return 0;
    let delivered = 0;
    for (const socket of sockets) {
      if (socket.readyState === 1) {
        send(socket, message);
        delivered += 1;
      }
    }
    return delivered;
  };

  wss.on('connection', (socket) => {
    const challenge = {
      type: 'auth_challenge',
      nonce: randomToken(24),
      expiresAt: Date.now() + 30000
    };
    let identity = null;
    send(socket, challenge);

    socket.on('message', async (raw) => {
      try {
        let message;
        try {
          message = JSON.parse(raw.toString());
        } catch {
          send(socket, { type: 'error', code: 'invalid_json' });
          return;
        }

        if (message.type === 'auth') {
          if (identity || message.nonce !== challenge.nonce || challenge.expiresAt < Date.now()) {
            send(socket, { type: 'auth_error', code: 'invalid_challenge' });
            return;
          }
          if (typeof message.chatwaveId !== 'string' || !/^CW-[A-HJ-NP-Z2-9]{8}$/.test(message.chatwaveId)) {
            send(socket, { type: 'auth_error', code: 'invalid_chatwave_id' });
            return;
          }
          let user;
          try {
            user = services.publicUser(db, message.chatwaveId);
          } catch (error) {
            send(socket, { type: 'auth_error', code: error.code || 'user_not_found' });
            return;
          }
          if (user.publicKeyVersion !== message.publicKeyVersion) {
            send(socket, { type: 'auth_error', code: 'key_version_mismatch' });
            return;
          }
          const unsigned = {
            type: 'auth',
            chatwaveId: message.chatwaveId,
            publicKeyVersion: message.publicKeyVersion,
            deviceId: typeof message.deviceId === 'string' ? message.deviceId.slice(0, 128) : '',
            nonce: message.nonce
          };
          if (!verifySignature(unsigned, message.signature, user.publicKey)) {
            send(socket, { type: 'auth_error', code: 'invalid_signature' });
            return;
          }
          if (!consumeNonce(db, message.nonce, 'ws_auth')) {
            send(socket, { type: 'auth_error', code: 'replayed_nonce' });
            return;
          }
          identity = {
            chatwaveId: user.chatwaveId,
            publicKey: user.publicKey,
            publicKeyVersion: user.publicKeyVersion,
            fingerprint: publicFingerprint(user.publicKey),
            deviceId: unsigned.deviceId
          };
          if (!socketsByUser.has(user.chatwaveId)) socketsByUser.set(user.chatwaveId, new Set());
          socketsByUser.get(user.chatwaveId).add(socket);
          try { services.updateDevicePresence(db, identity); } catch {}
          send(socket, { type: 'auth_ok', chatwaveId: user.chatwaveId, fingerprint: identity.fingerprint });
          return;
        }

      if (!identity) {
        send(socket, { type: 'error', code: 'not_authenticated' });
        return;
      }

      if (message.type === 'ping') {
        send(socket, { type: 'pong', sentAt: message.sentAt || Date.now() });
        return;
      }

      if (message.type === 'signal') {
        try {
          const body = message.envelope ? message : { envelope: message };
          const result = services.signal(db, identity, message.sessionId, body, config);
          const delivered = sendToUser(result.recipient, {
            type: 'signal',
            sessionId: message.sessionId,
            from: identity.chatwaveId,
            kind: result.kind,
            data: result.data,
            nonce: result.nonce
          });
          if (delivered === 0 && result.kind === 'offer') {
            await services.notifyUser(db, config, result.recipient, {
              type: 'incoming_call_signal',
              sessionId: message.sessionId,
              from: identity.chatwaveId
            });
          }
          send(socket, { type: 'signal_ack', sessionId: message.sessionId, nonce: result.nonce });
        } catch (error) {
          send(socket, { type: 'error', code: error.code || 'signal_failed', message: error.message });
        }
        return;
      }

        send(socket, { type: 'error', code: 'unsupported_message' });
      } catch (error) {
        console.error('[chatwave] realtime error', error && error.message);
        send(socket, { type: 'error', code: 'internal_error' });
      }
    });

    socket.on('close', () => {
      if (!identity) return;
      const sockets = socketsByUser.get(identity.chatwaveId);
      if (!sockets) return;
      sockets.delete(socket);
      if (sockets.size === 0) socketsByUser.delete(identity.chatwaveId);
    });

    socket.on('error', () => {});
  });

  return { sendToUser, wss };
}

module.exports = { attachRealtime };
