package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TopUserDto {
    private UUID   userId;
    private String username;
    private long   purchaseCount;
    private long   totalPez;
    private long   totalKreds;
}
