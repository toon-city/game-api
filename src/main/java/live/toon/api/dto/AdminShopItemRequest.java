package live.toon.api.dto;

public record AdminShopItemRequest(
    Long    itemId,
    String  shopId,
    Integer pezPrice,
    Integer kredBonus,
    Integer kredPrice,
    Boolean available,
    Integer stock,
    Long    collectionId
) {}
