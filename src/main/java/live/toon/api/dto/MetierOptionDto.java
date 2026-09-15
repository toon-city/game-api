package live.toon.api.dto;

import lombok.Builder;

/** One catalogue entry from the CURRENT user's point of view — used by the player-facing picker. */
@Builder
public record MetierOptionDto(
    Long id,
    String name,
    int dailyPez,
    Integer minToonizLevel,
    Integer minDaysPlayed,
    boolean eligible,
    /** Human-readable reason it's blocked (French, shown as-is), null when eligible. */
    String blockReason
) {}
