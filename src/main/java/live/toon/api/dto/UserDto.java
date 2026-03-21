package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class UserDto {
    private String username;
    /** MALE, FEMALE, NON_BINARY — peut être null */
    private String gender;
    /** 0 = user, 1 = modérateur, 2 = admin */
    private int rank;
    /** 0 = aucun, 1/2/3 */
    private int toonizLevel;
    private OffsetDateTime lastLoginAt;
}
