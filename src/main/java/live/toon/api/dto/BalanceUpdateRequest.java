package live.toon.api.dto;

/** Either field left null leaves that currency unchanged — set only what's edited. */
public record BalanceUpdateRequest(Integer pez, Integer kreds) {}
