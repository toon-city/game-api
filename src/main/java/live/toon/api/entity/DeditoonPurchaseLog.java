package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "deditoon_purchase_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeditoonPurchaseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deditoon_id", nullable = false)
    private Deditoon deditoon;

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
