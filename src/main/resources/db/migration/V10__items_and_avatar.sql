-- V10: Système d'items (catalogue, inventaire, boutiques) + persistence avatarOptions

-- ── Avatar options persisté en JSON ───────────────────────────────────────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_options JSONB NOT NULL DEFAULT '{}';

-- ── Catalogue d'items ─────────────────────────────────────────────────────────
-- item_type  : FURNITURE | CLOTHING | MISC
-- sub_type   : FLOOR | WALL | WALLPAPER | PIECE (meubles)
--              HAIRSTYLE | HAT | TOP | BOTTOM | MAKEUP (vêtements)
--              OTHER (divers)
-- possessable: false = ne génère pas de ligne user_items (ex: coiffures)
-- sprite_key : clé dans avatarOptions.clothing pour les vêtements
CREATE TABLE IF NOT EXISTS items (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(64)  NOT NULL,
    item_type     VARCHAR(20)  NOT NULL,
    sub_type      VARCHAR(30)  NOT NULL,
    possessable   BOOLEAN      NOT NULL DEFAULT TRUE,
    display_image VARCHAR(255) NOT NULL,
    sprite_path   VARCHAR(255),
    sprite_key    VARCHAR(64),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Possession d'items par un utilisateur ────────────────────────────────────
-- Un utilisateur peut posséder plusieurs fois le même item (une ligne par copie).
-- equipped         : true si un vêtement est actuellement porté
-- placed_in_room_id: non NULL si un meuble est posé dans une room
-- Un item équipé OU placé n'apparaît PAS dans l'inventaire visible.
CREATE TABLE IF NOT EXISTS user_items (
    id                BIGSERIAL   PRIMARY KEY,
    user_id           UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id           BIGINT      NOT NULL REFERENCES items(id),
    equipped          BOOLEAN     NOT NULL DEFAULT FALSE,
    placed_in_room_id BIGINT      REFERENCES rooms(id) ON DELETE SET NULL,
    acquired_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_items_user_id ON user_items (user_id);
CREATE INDEX IF NOT EXISTS idx_user_items_placed  ON user_items (placed_in_room_id) WHERE placed_in_room_id IS NOT NULL;

-- ── Catalogue boutique ────────────────────────────────────────────────────────
-- shop_id   : COUPE_TIFF | IKEBO | VESTIS
-- pez_price : prix en pez (NULL = pas d'option pez)
-- kred_bonus: kreds supplémentaires requis en plus du pez_price (0 = pez seul)
-- kred_price: prix en kreds seuls (NULL = pas d'option kreds)
-- Les deux options (pez+bonus / kreds seuls) peuvent coexister.
CREATE TABLE IF NOT EXISTS shop_items (
    id          BIGSERIAL    PRIMARY KEY,
    shop_id     VARCHAR(20)  NOT NULL,
    item_id     BIGINT       NOT NULL REFERENCES items(id),
    pez_price   INT,
    kred_bonus  INT          NOT NULL DEFAULT 0,
    kred_price  INT,
    available   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shop_items_shop_id ON shop_items (shop_id);
