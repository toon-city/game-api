-- V15: Couleur de peau comme colonne dédiée + suppression de avatar_options

-- Ajout de la colonne skin_color
ALTER TABLE users ADD COLUMN IF NOT EXISTS skin_color INT NULL;

-- Migration des données existantes : extraire skinColor depuis le JSONB avatar_options
UPDATE users
SET skin_color = (avatar_options->>'skinColor')::int
WHERE avatar_options IS NOT NULL
  AND avatar_options->>'skinColor' IS NOT NULL;

-- Suppression de la colonne avatar_options (remplacée par skin_color + user_items équipés)
ALTER TABLE users DROP COLUMN IF EXISTS avatar_options;
