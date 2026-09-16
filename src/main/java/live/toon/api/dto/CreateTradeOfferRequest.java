package live.toon.api.dto;

public record CreateTradeOfferRequest(
    Long offeredUserItemId,
    int offeredPez,
    Long requestedItemId,
    int requestedPez
) {}
