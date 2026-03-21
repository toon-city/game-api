package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsDto {
    private long onlineCount;
    private long registeredCount;
}
