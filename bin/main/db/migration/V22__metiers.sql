-- V22 : métiers (catalogue admin) + assignation utilisateur + compteur de jours joués

CREATE TABLE metiers (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(64) NOT NULL,
    daily_pez         INTEGER NOT NULL DEFAULT 0,
    -- NULL = pas de condition sur ce critère
    min_tooniz_level  INTEGER,
    min_days_played   INTEGER,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS metier_id BIGINT REFERENCES metiers(id) ON DELETE SET NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS metier_changed_at TIMESTAMPTZ;

-- Compteur "jours joués" : démarre à 1 à l'inscription, incrémenté au premier
-- requête authentifiée d'un jour calendaire encore jamais vu (voir
-- UserActivityService.touchDailyActivity). last_played_date NULL pour les
-- comptes existants : leur toute première requête après ce déploiement pose
-- juste la date sans incrémenter (déjà comptés via le défaut 1), exactement
-- le même comportement qu'une inscription toute fraîche.
ALTER TABLE users ADD COLUMN IF NOT EXISTS days_played INTEGER NOT NULL DEFAULT 1;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_played_date DATE;
