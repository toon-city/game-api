package live.toon.api.dto;

import java.util.UUID;

/** Body for POST /api/friends/requests and POST /api/friends/blocks — just a target user. */
public record FriendTargetRequest(UUID userId) {}
