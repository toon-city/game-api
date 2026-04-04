package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class KredsPackageDto {
    private Integer id;
    private String  name;
    private Integer kredsAmount;
    private Integer priceCents;
    private String  currency;
    private Boolean active;
    private OffsetDateTime createdAt;
}
