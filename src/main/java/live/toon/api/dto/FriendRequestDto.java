package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class FriendRequestDto {
    private Long id;
    private UUID otherUserId;
    private String otherUsername;
    private OffsetDateTime createdAt;
    private Integer skinColor;
    private Map<String, String> clothing;
}
