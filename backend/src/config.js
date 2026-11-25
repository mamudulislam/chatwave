'use strict';

const fs = require('node:fs');
const path = require('node:path');
require('dotenv').config();

const dbPath = path.resolve(process.cwd(), process.env.DB_PATH || './data/chatwave.sqlite');
fs.mkdirSync(path.dirname(dbPath), { recursive: true });

const parseList = (value, fallback = []) =>
  (value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .length
    ? (value || '').split(',').map((item) => item.trim()).filter(Boolean)
    : fallback;

module.exports = {
  port: Number(process.env.PORT || 8787),
  dbPath,
  turnSecret: process.env.TURN_SECRET || '',
  turnTtlSeconds: Number(process.env.TURN_TTL_SECONDS || 1800),
  turnUrls: parseList(process.env.TURN_URLS, ['stun:stun.l.google.com:19302']),
  unifiedPushTimeoutMs: Number(process.env.UNIFIEDPUSH_TIMEOUT_MS || 4000),
  signingMaxAgeSeconds: Number(process.env.SIGNING_MAX_AGE_SECONDS || 300),
  invitationTtlSeconds: Number(process.env.INVITATION_TTL_SECONDS || 300),
  callSessionTtlSeconds: Number(process.env.CALL_SESSION_TTL_SECONDS || 86400),
  callNotificationTtlSeconds: Number(process.env.CALL_NOTIFICATION_TTL_SECONDS || 30),
  rateLimitWindowSeconds: Number(process.env.RATE_LIMIT_WINDOW_SECONDS || 60),
  rateLimitMax: Number(process.env.RATE_LIMIT_MAX || 30),
  trustProxy: process.env.TRUST_PROXY === '1' || process.env.TRUST_PROXY === 'true',
  migrationDir: path.join(__dirname, '..', 'migrations')
};
