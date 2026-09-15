package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class BlockedUserDto {
    private UUID userId;
    private String username;
    private OffsetDateTime since;
    private Integer skinColor;
    private Map<String, String> clothing;
}
