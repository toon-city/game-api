-- Quantité limitée sur les articles de boutique (null = illimitée)
ALTER TABLE shop_items
    ADD COLUMN stock INT CHECK (stock >= 0);

-- Activation/désactivation d'une collection entière
ALTER TABLE item_collections
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
