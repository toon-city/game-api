package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "kreds_purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KredsPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private KredsPackage kredsPackage;

    /** Snapshot du montant de kreds reçu */
    @Column(name = "kreds_amount", nullable = false)
    private int kredsAmount;

    /** Snapshot du prix en centimes payé */
    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @CreationTimestamp
    @Column(name = "purchased_at", updatable = false)
    private OffsetDateTime purchasedAt;
}
