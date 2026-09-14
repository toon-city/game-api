package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MairieStatusDto {
    /** null if not married. */
    private String marriedToUsername;
    private List<MarriageProposalDto> sentProposals;
    private List<MarriageProposalDto> receivedProposals;
}
