package live.toon.api.dto;

import java.util.UUID;

public record ProposeMarriageRequest(UUID toUserId, Long ringUserItemId) {}
