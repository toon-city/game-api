package live.toon.api.dto;

import lombok.Builder;

/** Catalogue admin — pas d'éligibilité ici, voir MetierOptionDto pour la vue joueur. */
@Builder
public record MetierDto(
    Long id,
    String name,
    int dailyPez,
    Integer minToonizLevel,
    Integer minDaysPlayed
) {}
