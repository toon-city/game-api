package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class JustMarriedDto {
    private SpouseDto spouse1;
    private SpouseDto spouse2;
    private OffsetDateTime marriedAt;
}
