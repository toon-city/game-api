package live.toon.api.dto;

import java.time.OffsetDateTime;

/** bannedUntil null = permanent ban (unchanged prior behaviour). */
public record BanRequest(String reason, OffsetDateTime bannedUntil) {}
