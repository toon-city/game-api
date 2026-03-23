package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "shop_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "shop_id", nullable = false, length = 20)
    private ShopId shopId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /**
     * Prix en pez (null = pas d'option pez).
     * Si kredBonus > 0, le joueur paye pezPrice pez ET kredBonus kreds.
     */
    @Column(name = "pez_price")
    private Integer pezPrice;

    /** Kreds supplémentaires en plus du pez_price (0 = option pez seule). */
    @Column(name = "kred_bonus", nullable = false)
    @Builder.Default
    private int kredBonus = 0;

    /** Prix en kreds seuls (null = pas d'option kreds). */
    @Column(name = "kred_price")
    private Integer kredPrice;

    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;

    /** Quantité restante (null = stock illimité). */
    @Column
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private ItemCollection collection;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
