package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HouseDto {
    private Long id;
    private String name;
    private String type;    // "PRIVATE"
    private String access;  // "OPEN" | "PASSWORD" | "CLOSED"
    private String ownerUsername;
    private boolean hasPassword;
    private Long schemaId;
    private String schemaName;
    private int maxUsers;
    private int userCount;
}
