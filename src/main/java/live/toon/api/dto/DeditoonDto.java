package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class DeditoonDto {
    private long id;
    private String authorUsername;
    /** MALE | FEMALE | NON_BINARY | null */
    private String authorGender;
    private String message;
    private OffsetDateTime createdAt;
}
