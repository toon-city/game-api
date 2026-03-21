-- V8 : ajout de la colonne last_login_at sur la table users
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;
