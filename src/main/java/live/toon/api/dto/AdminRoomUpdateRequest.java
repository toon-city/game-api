package live.toon.api.dto;

public record AdminRoomUpdateRequest(
    String  name,
    String  type,
    String  access,
    String  houseData,
    String  ownerId,
    Integer maxUsers
) {}
