package live.toon.api.dto;

public record AdminMetierRequest(
    String name,
    int dailyPez,
    Integer minToonizLevel,
    Integer minDaysPlayed,
    Long outfitTshirtItemId,
    Long outfitPantItemId,
    Long outfitHatItemId
) {}
