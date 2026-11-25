CREATE TABLE IF NOT EXISTS users (
  chatwave_id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  public_key TEXT NOT NULL,
  public_key_version INTEGER NOT NULL DEFAULT 1,
  recovery_hash TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  revoked INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS devices (
  device_id TEXT PRIMARY KEY,
  chatwave_id TEXT NOT NULL REFERENCES users(chatwave_id) ON DELETE CASCADE,
  display_name TEXT NOT NULL,
  platform TEXT NOT NULL DEFAULT 'android',
  created_at INTEGER NOT NULL,
  last_seen INTEGER NOT NULL,
  revoked INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS device_keys (
  chatwave_id TEXT NOT NULL REFERENCES users(chatwave_id) ON DELETE CASCADE,
  version INTEGER NOT NULL,
  public_key TEXT NOT NULL,
  fingerprint TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  active INTEGER NOT NULL DEFAULT 1,
  PRIMARY KEY (chatwave_id, version)
);

CREATE TABLE IF NOT EXISTS invitations (
  code_hash TEXT PRIMARY KEY,
  inviter_id TEXT NOT NULL REFERENCES users(chatwave_id) ON DELETE CASCADE,
  expires_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL,
  used_at INTEGER,
  canceled_at INTEGER,
  redeemed_by TEXT,
  attempts INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS trusted_contacts (
  owner_id TEXT NOT NULL REFERENCES users(chatwave_id) ON DELETE CASCADE,
  contact_id TEXT NOT NULL REFERENCES users(chatwave_id) ON DELETE CASCADE,
  verified INTEGER NOT NULL DEFAULT 0,
  safety_code TEXT,
  key_version INTEGER NOT NULL DEFAULT 1,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  PRIMARY KEY (owner_id, contact_id)
);

CREATE TABLE IF NOT EXISTS call_sessions (
  id TEXT PRIMARY KEY,
  caller_id TEXT NOT NULL REFERENCES users(chatwave_id) ON DELETE CASCADE,
  callee_id TEXT NOT NULL REFERENCES users(chatwave_id) ON DELETE CASCADE,
  state TEXT NOT NULL,
  safety_binding TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  ended_at INTEGER
);

CREATE TABLE IF NOT EXISTS call_participants (
  session_id TEXT NOT NULL REFERENCES call_sessions(id) ON DELETE CASCADE,
  chatwave_id TEXT NOT NULL REFERENCES users(chatwave_id) ON DELETE CASCADE,
  role TEXT NOT NULL,
  fingerprint TEXT NOT NULL,
  ephemeral_public_key TEXT NOT NULL,
  joined_at INTEGER NOT NULL,
  PRIMARY KEY (session_id, chatwave_id)
);

CREATE TABLE IF NOT EXISTS call_events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id TEXT NOT NULL REFERENCES call_sessions(id) ON DELETE CASCADE,
  type TEXT NOT NULL,
  sender_id TEXT NOT NULL,
  payload TEXT NOT NULL,
  nonce TEXT NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS push_devices (
  endpoint_hash TEXT PRIMARY KEY,
  chatwave_id TEXT NOT NULL REFERENCES users(chatwave_id) ON DELETE CASCADE,
  endpoint TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  last_seen INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS nonces (
  nonce TEXT PRIMARY KEY,
  kind TEXT NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS rate_limits (
  key TEXT PRIMARY KEY,
  count INTEGER NOT NULL,
  window_start INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_call_sessions_participants ON call_sessions(caller_id, callee_id, created_at);
CREATE INDEX IF NOT EXISTS idx_call_events_session ON call_events(session_id, created_at);
CREATE INDEX IF NOT EXISTS idx_invitations_expiry ON invitations(expires_at);
CREATE INDEX IF NOT EXISTS idx_devices_user ON devices(chatwave_id);
