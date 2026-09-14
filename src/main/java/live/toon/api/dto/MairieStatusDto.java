package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class MairieStatusDto {
    /** null if not married. */
    private String marriedToUsername;
    /** null if not married. */
    private OffsetDateTime marriedAt;
    /** null if not married — enough to render the spouse's avatar (see SpouseDto). */
    private SpouseDto spouse;
    private List<MarriageProposalDto> sentProposals;
    private List<MarriageProposalDto> receivedProposals;
}
