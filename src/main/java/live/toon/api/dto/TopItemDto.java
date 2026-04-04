package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopItemDto {
    private Long   itemId;
    private String name;
    private String displayImage;
    private long   purchaseCount;
}
