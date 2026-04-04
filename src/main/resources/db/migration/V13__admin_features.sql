-- ── Ban fields on users ────────────────────────────────────────────────────────
ALTER TABLE users
    ADD COLUMN banned          BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN ban_reason      VARCHAR(500),
    ADD COLUMN banned_at       TIMESTAMPTZ,
    ADD COLUMN banned_by_id    UUID         REFERENCES users(id) ON DELETE SET NULL;

-- ── Room lock (réservé aux admins, non contrôlable par le propriétaire) ────────
ALTER TABLE rooms
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE;

-- ── Kreds packages (offres d'achat de monnaie payante) ────────────────────────
CREATE TABLE kreds_packages (
    id           SERIAL      PRIMARY KEY,
    kreds_amount INT         NOT NULL CHECK (kreds_amount > 0),
    price_cents  INT         NOT NULL CHECK (price_cents > 0),
    currency     VARCHAR(3)  NOT NULL DEFAULT 'EUR',
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Kreds purchases (achat de monnaie virtuelle par un utilisateur) ───────────
CREATE TABLE kreds_purchases (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    package_id   INT         NOT NULL REFERENCES kreds_packages(id),
    kreds_amount INT         NOT NULL,
    price_cents  INT         NOT NULL,
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kreds_purchases_user_id      ON kreds_purchases(user_id);
CREATE INDEX idx_kreds_purchases_purchased_at ON kreds_purchases(purchased_at);

-- ── Purchase logs (achats d'articles en boutique) ────────────────────────────
CREATE TABLE purchase_logs (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_item_id BIGINT      NOT NULL REFERENCES shop_items(id),
    item_id      BIGINT      NOT NULL REFERENCES items(id),
    buy_option   VARCHAR(10) NOT NULL,
    pez_spent    INT         NOT NULL DEFAULT 0,
    kreds_spent  INT         NOT NULL DEFAULT 0,
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_purchase_logs_user_id      ON purchase_logs(user_id);
CREATE INDEX idx_purchase_logs_purchased_at ON purchase_logs(purchased_at);
CREATE INDEX idx_purchase_logs_item_id      ON purchase_logs(item_id);

-- ── Deditoon purchase logs (envoi de déditoons payants) ──────────────────────
CREATE TABLE deditoon_purchase_logs (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    deditoon_id  BIGINT      NOT NULL REFERENCES deditoons(id),
    pez_spent    INT         NOT NULL DEFAULT 0,
    kreds_spent  INT         NOT NULL DEFAULT 0,
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_deditoon_pl_user_id      ON deditoon_purchase_logs(user_id);
CREATE INDEX idx_deditoon_pl_purchased_at ON deditoon_purchase_logs(purchased_at);

-- ── Connection logs (historique des connexions) ───────────────────────────────
CREATE TABLE connection_logs (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_connection_logs_user_id      ON connection_logs(user_id);
CREATE INDEX idx_connection_logs_connected_at ON connection_logs(connected_at);
