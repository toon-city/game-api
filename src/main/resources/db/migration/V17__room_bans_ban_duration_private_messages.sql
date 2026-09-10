-- Site ban gains a duration: NULL = permanent (existing behaviour unchanged),
-- a timestamp = auto-lifted on next login attempt after it passes (see
-- AuthService.login()).
ALTER TABLE users ADD COLUMN IF NOT EXISTS banned_until TIMESTAMPTZ;

-- Per-room ban: blocks re-joining one specific room, independent of the
-- site-wide ban above. One row per (room, user) — banning again just
-- updates the reason/timestamp.
CREATE TABLE room_bans (
    id           BIGSERIAL PRIMARY KEY,
    room_id      BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_by_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reason       VARCHAR(500),
    banned_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (room_id, user_id)
);

-- Private messages sent via the user-action dialog. Write-only for now —
-- persisted for moderation/audit trail, no read endpoint yet.
CREATE TABLE private_messages (
    id           BIGSERIAL PRIMARY KEY,
    room_id      BIGINT REFERENCES rooms(id) ON DELETE SET NULL,
    from_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message      TEXT NOT NULL,
    sent_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_private_messages_participants ON private_messages(from_user_id, to_user_id, sent_at DESC);
