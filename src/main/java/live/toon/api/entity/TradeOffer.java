package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A public "give item X (+pez) for item-type Y (+pez)" offer — same shape
 * as MarriageProposal (raw UUID FKs for the two people, a real
 * @ManyToOne for the item in play, status enum + createdAt/resolvedAt),
 * except there's no specific recipient: any user owning an unequipped Y
 * can accept. See TradeService for the full state machine.
 */
@Entity
@Table(name = "trade_offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offerer_id", nullable = false)
    private UUID offererId;

    /** The specific inventory item being given away — locked (see TradeOfferRepository.existsByOfferedUserItemIdAndStatus) while OPEN. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_user_item_id", nullable = false)
    private UserItem offeredUserItem;

    /** Escrowed from the offerer at creation time — see WorkOutfitService-style comment in TradeService.propose(). */
    @Column(name = "offered_pez", nullable = false)
    @Builder.Default
    private int offeredPez = 0;

    /** A catalog item TYPE, not a specific instance — any unequipped copy of it qualifies at accept time. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_item_id", nullable = false)
    private Item requestedItem;

    /** Only checked/debited from whoever accepts, never reserved in advance (we don't know who that'll be). */
    @Column(name = "requested_pez", nullable = false)
    @Builder.Default
    private int requestedPez = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TradeOfferStatus status = TradeOfferStatus.OPEN;

    @Column(name = "accepted_by_id")
    private UUID acceptedById;

    /** The acceptor's matching item, transferred to the offerer — kept for history display. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_user_item_id")
    private UserItem acceptedUserItem;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
}
