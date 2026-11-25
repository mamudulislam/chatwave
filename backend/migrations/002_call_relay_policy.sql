ALTER TABLE call_sessions ADD COLUMN relay_only INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_call_sessions_expiry ON call_sessions(expires_at);