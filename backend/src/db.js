'use strict';

const fs = require('node:fs');
const path = require('node:path');
const Database = require('better-sqlite3');

function openDatabase(filename, migrationDir) {
  const db = new Database(filename);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');
  db.exec(`
    CREATE TABLE IF NOT EXISTS schema_migrations (
      name TEXT PRIMARY KEY,
      applied_at INTEGER NOT NULL
    )
  `);
  applyMigrations(db, migrationDir);
  return db;
}

function applyMigrations(db, migrationDir) {
  const files = fs
    .readdirSync(migrationDir)
    .filter((name) => name.endsWith('.sql'))
    .sort();
  for (const file of files) {
    const applied = db.prepare('SELECT 1 FROM schema_migrations WHERE name = ?').get(file);
    if (applied) continue;
    const sql = fs.readFileSync(path.join(migrationDir, file), 'utf8');
    db.transaction(() => {
      db.exec(sql);
      db.prepare('INSERT INTO schema_migrations (name, applied_at) VALUES (?, ?)').run(file, Date.now());
    })();
  }
}

function consumeNonce(db, nonce, kind, now = Date.now()) {
  try {
    db.prepare('INSERT INTO nonces (nonce, kind, created_at) VALUES (?, ?, ?)').run(nonce, kind, now);
    return true;
  } catch (error) {
    if (String(error.code || '').startsWith('SQLITE_CONSTRAINT')) return false;
    throw error;
  }
}

function checkRateLimit(db, key, max, windowSeconds, now = Date.now()) {
  const windowStart = now - windowSeconds * 1000;
  return db.transaction(() => {
    const existing = db.prepare('SELECT count, window_start FROM rate_limits WHERE key = ?').get(key);
    if (!existing || existing.window_start < windowStart) {
      db.prepare(`
        INSERT INTO rate_limits (key, count, window_start) VALUES (?, 1, ?)
        ON CONFLICT(key) DO UPDATE SET count = 1, window_start = excluded.window_start
      `).run(key, now);
      return true;
    }
    if (existing.count >= max) return false;
    db.prepare('UPDATE rate_limits SET count = count + 1 WHERE key = ?').run(key);
    return true;
  })();
}

function cleanupExpired(db, now = Date.now()) {
  db.prepare('DELETE FROM invitations WHERE expires_at < ? AND used_at IS NULL').run(now - 86400000);
  db.prepare("UPDATE call_sessions SET state = 'missed', ended_at = COALESCE(ended_at, ?) WHERE state = 'ringing' AND expires_at < ?").run(now, now);
  db.prepare('DELETE FROM call_sessions WHERE created_at < ?').run(now - 86400000 * 7);
  db.prepare('DELETE FROM call_events WHERE created_at < ?').run(now - 3600000);
  db.prepare('DELETE FROM nonces WHERE created_at < ?').run(now - 3600000);
  db.prepare('DELETE FROM rate_limits WHERE window_start < ?').run(now - 86400000);
}

module.exports = {
  openDatabase,
  applyMigrations,
  consumeNonce,
  checkRateLimit,
  cleanupExpired
};

if (require.main === module) {
  const config = require('./config');
  const db = openDatabase(config.dbPath, config.migrationDir);
  db.close();
  console.log(`[chatwave] migrations applied at ${config.dbPath}`);
}
