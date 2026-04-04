package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminUserDto {
    private UUID   id;
    private String username;
    private String email;
    private String gender;
    private int    rank;
    private int    toonizLevel;
    private int    kreds;
    private int    pez;
    private boolean online;
    private boolean banned;
    private String  banReason;
    private OffsetDateTime bannedAt;
    private UUID           bannedById;
    private String         bannedByUsername;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLoginAt;
}
