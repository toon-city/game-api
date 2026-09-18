-- Ajout de la colonne hair_color, même schéma que skin_color (V15).
ALTER TABLE users ADD COLUMN IF NOT EXISTS hair_color INT NULL;
