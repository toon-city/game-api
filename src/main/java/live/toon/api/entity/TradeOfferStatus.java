package live.toon.api.entity;

/** No DECLINED — a public offer isn't refused by anyone specific, only cancelled by its owner or accepted. */
public enum TradeOfferStatus {
    OPEN,
    ACCEPTED,
    CANCELLED
}
