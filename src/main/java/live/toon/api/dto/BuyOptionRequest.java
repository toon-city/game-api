package live.toon.api.dto;

public record BuyOptionRequest(BuyOption option) {
    public enum BuyOption {
        /** Payer en pez (+ kredBonus kreds si applicable) */
        PEZ,
        /** Payer en kreds seuls */
        KREDS
    }
}
