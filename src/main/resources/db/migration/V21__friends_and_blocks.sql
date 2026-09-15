CREATE TABLE friend_requests (
    id BIGSERIAL PRIMARY KEY,
    from_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);
CREATE INDEX idx_friend_requests_to_status ON friend_requests(to_user_id, status);
CREATE INDEX idx_friend_requests_from_status ON friend_requests(from_user_id, status);

CREATE TABLE friendships (
    id BIGSERIAL PRIMARY KEY,
    -- user_a_id < user_b_id (string compare on the UUID) — canonical order so
    -- a pair only ever has one row regardless of who friended whom.
    user_a_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_b_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id)
);
CREATE INDEX idx_friendships_a ON friendships(user_a_id);
CREATE INDEX idx_friendships_b ON friendships(user_b_id);

CREATE TABLE user_blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (blocker_id, blocked_id)
);
CREATE INDEX idx_user_blocks_blocker ON user_blocks(blocker_id);
