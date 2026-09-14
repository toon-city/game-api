package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class MarriageProposalDto {
    private Long id;
    private UUID otherUserId;
    private String otherUsername;
    private OffsetDateTime createdAt;
    private String ringName;
    private String ringDisplayImage;
}
