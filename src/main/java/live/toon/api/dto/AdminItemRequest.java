package live.toon.api.dto;

public record AdminItemRequest(
    String name,
    String itemType,
    String subType,
    boolean possessable,
    String displayImage,
    String spritePath,
    String spriteKey
) {}
