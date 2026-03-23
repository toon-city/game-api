package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class UserItemDto {
    private Long id;
    private ItemDto item;
    private boolean equipped;
    /** ID de la room dans laquelle le meuble est placé, null sinon */
    private Long placedInRoomId;
    private OffsetDateTime acquiredAt;
}
