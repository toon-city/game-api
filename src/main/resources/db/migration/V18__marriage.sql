ALTER TABLE users ADD COLUMN married_to_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE users ADD COLUMN married_at TIMESTAMPTZ;

CREATE TABLE marriage_proposals (
    id                BIGSERIAL PRIMARY KEY,
    from_user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ring_user_item_id BIGINT NOT NULL REFERENCES user_items(id) ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED, CANCELLED
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at       TIMESTAMPTZ,
    CHECK (from_user_id <> to_user_id)
);
CREATE INDEX idx_marriage_proposals_to   ON marriage_proposals(to_user_id, status);
CREATE INDEX idx_marriage_proposals_from ON marriage_proposals(from_user_id, status);
