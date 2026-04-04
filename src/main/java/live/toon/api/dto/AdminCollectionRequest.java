package live.toon.api.dto;

public record AdminCollectionRequest(
    String  shopId,
    String  name,
    String  bannerImage,
    Integer sortOrder,
    Boolean enabled
) {}
