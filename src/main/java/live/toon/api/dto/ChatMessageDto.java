package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageDto {
    private Long id;
    private Long roomId;
    private String userId;
    private String username;
    private String message;
    private String sentAt;
}
