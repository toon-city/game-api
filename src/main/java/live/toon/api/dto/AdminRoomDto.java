package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class AdminRoomDto {
    private Long   id;
    private String name;
    private String houseData;
    private int    maxUsers;
    private int    userCount;
    private String type;
    private String access;
    private boolean locked;
    private AdminUserDto owner;
    private OffsetDateTime createdAt;
}
