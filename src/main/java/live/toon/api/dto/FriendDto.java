package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class FriendDto {
    private UUID userId;
    private String username;
    private OffsetDateTime since;
}
