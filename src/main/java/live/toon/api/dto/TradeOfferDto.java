package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class TradeOfferDto {
    private Long id;
    private UUID offererId;
    private String offererUsername;
    /** The specific item given away, as its catalog definition (name/image) — not the UserItem instance. */
    private ItemDto offeredItem;
    private int offeredPez;
    /** The item TYPE requested in return — any unequipped copy qualifies at accept time. */
    private ItemDto requestedItem;
    private int requestedPez;
    /** OPEN, ACCEPTED, CANCELLED */
    private String status;
    private OffsetDateTime createdAt;
    /** Null unless ACCEPTED. */
    private String acceptedByUsername;
    private OffsetDateTime resolvedAt;
}
