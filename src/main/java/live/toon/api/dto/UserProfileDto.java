package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Full player profile page — richer than UserDto (the paginated player-list row shape). */
@Data
@Builder
public class UserProfileDto {
    private UUID id;
    private String username;
    private String gender;
    /** 0 = joueur, 1 = modérateur, 2 = admin. */
    private int rank;
    /** 0 = pas tooniz, 1-3 = niveau. */
    private int toonizLevel;
    private OffsetDateTime createdAt;
    private String job;
    private String description;
    private Integer skinColor;
    /** spriteKey -> spritePath, same shape as SpouseDto.clothing — for AvatarBadgeComponent's [override]. */
    private Map<String, String> clothing;
    /** null if not married. */
    private String marriedToUsername;
    private OffsetDateTime marriedAt;
    /** Every equipped item, sprite or not (a ring has none) — the profile's "slots" around the avatar. */
    private List<EquippedItemDto> equippedItems;
}
