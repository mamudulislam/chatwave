'use strict';

const http = require('node:http');
const express = require('express');
const config = require('./config');
const { openDatabase, cleanupExpired, checkRateLimit } = require('./db');
const services = require('./services');
const { attachRealtime } = require('./realtime');
const { HttpError, unauthorized } = require('./errors');
const { verifyEnvelope } = require('./signing');

const asyncHandler = (handler) => (req, res, next) => Promise.resolve(handler(req, res, next)).catch(next);

function createApp(options = {}) {
  const activeConfig = { ...config, ...(options.config || {}) };
  const db = options.db || openDatabase(activeConfig.dbPath, activeConfig.migrationDir);
  const app = express();
  app.disable('x-powered-by');
  if (activeConfig.trustProxy) app.set('trust proxy', activeConfig.trustProxy);
  app.use(express.json({ limit: '256kb' }));

  const server = http.createServer(app);
  const context = { db, config: activeConfig, realtime: null, notify: null };

  const requireIdentity = (req, _res, next) => {
    const fromId = req.body && req.body.envelope && req.body.envelope.fromId;
    if (!fromId) return next(unauthorized('identity_required'));
    try {
      req.user = services.publicUser(db, fromId);
      const deviceId = req.headers['x-chatwave-device'];
      req.user.deviceId = typeof deviceId === 'string' ? deviceId.slice(0, 128) : '';
      return next();
    } catch (error) {
      return next(error);
    }
  };

  app.get('/health', (_req, res) => res.json({ ok: true, service: 'chatwave', time: Date.now() }));

  app.post('/api/users/register', asyncHandler(async (req, res) => {
    if (!checkRateLimit(db, `register:${req.ip}`, activeConfig.rateLimitMax, activeConfig.rateLimitWindowSeconds)) {
      throw new HttpError(429, 'rate_limited');
    }
    res.json(services.registerUser(db, req.body, activeConfig));
  }));

  app.post('/api/identity/recover', asyncHandler(async (req, res) => {
    if (!checkRateLimit(db, `recover:${req.ip}`, 10, 3600)) throw new HttpError(429, 'rate_limited');
    res.json(services.recoverIdentity(db, req.body, activeConfig));
  }));

  app.get('/api/users/:id', (req, res, next) => {
    try {
      res.json(services.lookupUser(db, req.params.id));
    } catch (error) {
      next(error);
    }
  });

  app.post('/api/invitations', requireIdentity, (req, res, next) => {
    try {
      res.status(201).json(services.createInvitation(db, req.user, req.body, activeConfig));
    } catch (error) {
      next(error);
    }
  });

  app.post('/api/invitations/redeem', requireIdentity, (req, res, next) => {
    try {
      res.json(services.redeemInvitation(db, req.user, req.body, activeConfig));
    } catch (error) {
      next(error);
    }
  });

  app.post('/api/invitations/cancel', requireIdentity, (req, res, next) => {
    try {
      res.json(services.cancelInvitation(db, req.user, req.body, activeConfig));
    } catch (error) {
      next(error);
    }
  });

  app.post('/api/calls', requireIdentity, asyncHandler(async (req, res) => {
    const session = await services.createCall(db, req.user, req.body, activeConfig);
    await context.notify(session.participants.find((p) => p.chatwave_id !== req.user.chatwaveId).chatwave_id, {
      type: 'incoming_call',
      sessionId: session.id,
      caller: services.publicUser(db, req.user.chatwaveId),
      safetyCode: services.safetyCodeFor(db, req.user.chatwaveId, session.participants.find((p) => p.chatwave_id !== req.user.chatwaveId).chatwave_id),
      relayOnly: Boolean(session.relayOnly),
      expiresAt: Math.min(session.expiresAt, Date.now() + activeConfig.callNotificationTtlSeconds * 1000)
    });
    res.status(201).json(session);
  }));

  app.post('/api/calls/:id/status', requireIdentity, (req, res, next) => {
    try {
      const body = { ...req.body, envelope: { ...req.body.envelope, sessionId: req.params.id } };
      const result = verifyEnvelope(db, body.envelope, req.user.publicKey, 'call.status', activeConfig);
      if (!result.ok || result.payload.sessionId !== req.params.id) {
        return next(new HttpError(400, result.ok ? 'envelope_payload_mismatch' : result.reason));
      }
      return res.json(services.callSession(db, req.user, req.params.id));
    } catch (error) {
      return next(error);
    }
  });

  app.post('/api/calls/:id/accept', requireIdentity, (req, res, next) => {
    try {
      const body = { ...req.body, envelope: { ...req.body.envelope, sessionId: req.params.id } };
      res.json(services.acceptCall(db, req.user, req.params.id, body, activeConfig));
    } catch (error) {
      next(error);
    }
  });

  app.post('/api/calls/:id/decline', requireIdentity, (req, res, next) => {
    try {
      const body = { ...req.body, envelope: { ...req.body.envelope, sessionId: req.params.id } };
      res.json(services.updateCallState(db, req.user, req.params.id, body, activeConfig, 'call.decline', 'declined'));
    } catch (error) {
      next(error);
    }
  });

  app.post('/api/calls/:id/end', requireIdentity, (req, res, next) => {
    try {
      const body = { ...req.body, envelope: { ...req.body.envelope, sessionId: req.params.id } };
      res.json(services.updateCallState(db, req.user, req.params.id, body, activeConfig, 'call.end', 'ended'));
    } catch (error) {
      next(error);
    }
  });

  app.post('/api/calls/:id/signal', requireIdentity, asyncHandler(async (req, res) => {
    const body = { ...req.body, envelope: { ...req.body.envelope, sessionId: req.params.id } };
    const result = services.signal(db, req.user, req.params.id, body, activeConfig);
    const delivered = context.realtime ? context.realtime.sendToUser(result.recipient, {
      type: 'signal',
      sessionId: req.params.id,
      from: req.user.chatwaveId,
      kind: result.kind,
      data: result.data,
      nonce: result.nonce
    }) : 0;
    if (delivered === 0 && result.kind === 'offer') {
      await services.notifyUser(db, activeConfig, result.recipient, {
        type: 'incoming_call_signal',
        sessionId: req.params.id,
        from: req.user.chatwaveId
      });
    }
    res.json({ sent: true, delivered });
  }));

  app.post('/api/turn/credentials', requireIdentity, (req, res, next) => {
    try {
      res.json(services.turnCredentialsFor(db, req.user, req.body, activeConfig));
    } catch (error) {
      next(error);
    }
  });

  app.post('/api/push/register', requireIdentity, (req, res, next) => {
    try {
      res.json(services.registerPush(db, req.user, req.body, activeConfig));
    } catch (error) {
      next(error);
    }
  });

  app.use((_req, res) => res.status(404).json({ code: 'not_found' }));

  app.use((error, _req, res, _next) => {
    if (error instanceof HttpError) {
      res.status(error.status).json({ code: error.code, message: error.message });
      return;
    }
    if (String(error.code || '').startsWith('SQLITE_CONSTRAINT')) {
      res.status(409).json({ code: 'conflict', message: 'Constraint violation' });
      return;
    }
    console.error('[chatwave] unhandled error', error.message);
    res.status(500).json({ code: 'internal_error' });
  });

  context.realtime = attachRealtime(server, context);
  context.notify = async (userId, message) => {
    const delivered = context.realtime ? context.realtime.sendToUser(userId, message) : 0;
    if (delivered === 0) {
      await services.notifyUser(db, activeConfig, userId, message);
    }
    return delivered;
  };

  return { app, server, db, config: activeConfig, context };
}

function startServer() {
  const instance = createApp();
  const cleanup = setInterval(() => cleanupExpired(instance.db), 3600000);
  cleanup.unref();
  instance.server.listen(instance.config.port, () => {
    console.log(`[chatwave] backend listening on :${instance.config.port}`);
  });
  return instance;
}

if (require.main === module) startServer();

module.exports = { createApp, startServer };
