-- V3: ajout du genre utilisateur
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
