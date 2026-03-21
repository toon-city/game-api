package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HouseSchemaDto {
    private Long id;
    private String name;
    private String description;
}
