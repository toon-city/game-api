-- V24 : centre d'échange — offres publiques "mon item (+pez) contre un type
-- d'item précis (+pez)". Voir TradeOffer.java / TradeService.java.

CREATE TABLE trade_offers (
    id                     BIGSERIAL PRIMARY KEY,
    offerer_id             UUID NOT NULL REFERENCES users(id),
    offered_user_item_id   BIGINT NOT NULL REFERENCES user_items(id),
    offered_pez            INTEGER NOT NULL DEFAULT 0,
    requested_item_id      BIGINT NOT NULL REFERENCES items(id),
    requested_pez          INTEGER NOT NULL DEFAULT 0,
    status                 VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    accepted_by_id         UUID REFERENCES users(id),
    accepted_user_item_id  BIGINT REFERENCES user_items(id),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at            TIMESTAMPTZ
);

CREATE INDEX idx_trade_offers_status ON trade_offers(status);
CREATE INDEX idx_trade_offers_offerer ON trade_offers(offerer_id);
