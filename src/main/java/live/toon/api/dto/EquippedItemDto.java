package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

/** One equipped item slot on a profile — includes items with no sprite (e.g. a ring). */
@Data
@Builder
public class EquippedItemDto {
    private Long userItemId;
    /** HAIRSTYLE, HAT, TOP, BOTTOM, MAKEUP, RING, ... */
    private String subType;
    private String name;
    private String displayImage;
}
