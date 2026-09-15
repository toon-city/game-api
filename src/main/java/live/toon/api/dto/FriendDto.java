package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class FriendDto {
    private UUID userId;
    private String username;
    private OffsetDateTime since;
    /** Enough to render their avatar (head crop) in the friends list — same shape as SpouseDto. */
    private Integer skinColor;
    private Map<String, String> clothing;
}
