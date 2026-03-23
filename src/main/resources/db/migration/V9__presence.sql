-- V9__presence.sql
-- Persistance de la présence des joueurs et du nombre de connectés par room

-- Présence utilisateur
ALTER TABLE users
    ADD COLUMN online           BOOLEAN  NOT NULL DEFAULT FALSE,
    ADD COLUMN current_room_id  BIGINT   REFERENCES rooms(id) ON DELETE SET NULL;

-- Compteur de joueurs présents dans chaque room
ALTER TABLE rooms
    ADD COLUMN user_count INT NOT NULL DEFAULT 0;

-- S'assurer que toutes les rooms existantes sont à 0 (cohérence au démarrage)
UPDATE rooms SET user_count = 0;
UPDATE users SET online = FALSE, current_room_id = NULL;

CREATE INDEX idx_users_online        ON users(online);
CREATE INDEX idx_users_current_room  ON users(current_room_id);
CREATE INDEX idx_rooms_user_count    ON rooms(user_count DESC);
