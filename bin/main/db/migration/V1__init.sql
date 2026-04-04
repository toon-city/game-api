-- V1__init.sql
-- Toon Live initial schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(32) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE rooms (
    id          BIGSERIAL   PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    house_xml   TEXT,
    max_users   INT         NOT NULL DEFAULT 50,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_messages (
    id          BIGSERIAL   PRIMARY KEY,
    room_id     BIGINT      NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username    VARCHAR(32) NOT NULL,
    message     TEXT        NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_room_id ON chat_messages(room_id, sent_at DESC);

-- Seed: default room "Jardin"
INSERT INTO rooms (name, house_xml, max_users)
VALUES ('Jardin', NULL, 100);
