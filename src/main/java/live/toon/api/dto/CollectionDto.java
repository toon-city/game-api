package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollectionDto {
    private Long id;
    private String shopId;
    private String name;
    private String bannerImage;
    private int sortOrder;
    private boolean enabled;
}
