package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchase_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private ShopItem shopItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** "PEZ" ou "KREDS" */
    @Column(name = "buy_option", nullable = false, length = 10)
    private String buyOption;

    @Column(name = "pez_spent", nullable = false)
    @Builder.Default
    private int pezSpent = 0;

    @Column(name = "kreds_spent", nullable = false)
    @Builder.Default
    private int kredsSpent = 0;

    @CreationTimestamp
    @Column(name = "purchased_at", updatable = false)
    private OffsetDateTime purchasedAt;
}
