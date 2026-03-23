-- Collections pour les articles de boutique
-- Une collection appartient à une boutique et peut avoir une bannière
CREATE TABLE item_collections (
    id           BIGSERIAL    PRIMARY KEY,
    shop_id      VARCHAR(20)  NOT NULL,
    name         VARCHAR(64)  NOT NULL,
    banner_image VARCHAR(255),
    sort_order   INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_item_collections_shop_id ON item_collections (shop_id);

-- Un shop_item peut appartenir à une collection (nullable)
ALTER TABLE shop_items
    ADD COLUMN collection_id BIGINT REFERENCES item_collections (id) ON DELETE SET NULL;

CREATE INDEX idx_shop_items_collection_id ON shop_items (collection_id);
