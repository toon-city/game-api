package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomDto {
    private Long id;
    private String name;
    private String houseData;
    private int maxUsers;
    private int userCount;
    // Public rooms only — type is always "PUBLIC" here
    private String type;
}
