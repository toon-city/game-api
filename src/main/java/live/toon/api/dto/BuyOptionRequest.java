package live.toon.api.dto;

/** quantity: null or < 1 treated as 1 by the service (also clamped to 1 for
 *  non-possessable items — equipping "3 hairstyles" makes no sense). */
public record BuyOptionRequest(BuyOption option, Integer quantity) {
    public enum BuyOption {
        /** Payer en pez (+ kredBonus kreds si applicable) */
        PEZ,
        /** Payer en kreds seuls */
        KREDS
    }
}
