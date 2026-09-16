-- V23 : tenue de travail d'un métier (tshirt + pantalon obligatoires côté
-- formulaire admin, chapeau optionnel — nullable ici pour ne pas casser les
-- métiers existants créés avant cette colonne) + interrupteur par joueur.

ALTER TABLE metiers ADD COLUMN IF NOT EXISTS outfit_tshirt_item_id BIGINT REFERENCES items(id) ON DELETE SET NULL;
ALTER TABLE metiers ADD COLUMN IF NOT EXISTS outfit_pant_item_id   BIGINT REFERENCES items(id) ON DELETE SET NULL;
ALTER TABLE metiers ADD COLUMN IF NOT EXISTS outfit_hat_item_id    BIGINT REFERENCES items(id) ON DELETE SET NULL;

-- Overlay, pas un déséquipement : quand true, tshirt/pant/hat rendus sont
-- ceux de la tenue du métier plutôt que des items réellement équipés (qui
-- restent inchangés en base — voir RoomStateService.buildClothingMap côté
-- game-server-java et ProfileService.effectiveClothing côté game-api).
ALTER TABLE users ADD COLUMN IF NOT EXISTS work_outfit_active BOOLEAN NOT NULL DEFAULT false;
