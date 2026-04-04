package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "kreds_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KredsPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String name = "";

    @Column(name = "kreds_amount", nullable = false)
    private int kredsAmount;

    /** Prix en centimes (ex: 499 = 4,99 €) */
    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "EUR";

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
