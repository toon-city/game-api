-- V7: Devises joueur (kreds / pez) + table déditoons

ALTER TABLE users ADD COLUMN IF NOT EXISTS kreds INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS pez   INT NOT NULL DEFAULT 1500;

CREATE TABLE IF NOT EXISTS deditoons (
    id          BIGSERIAL    PRIMARY KEY,
    author_id   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message     VARCHAR(150) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS deditoons_created_at_idx ON deditoons (created_at DESC);
