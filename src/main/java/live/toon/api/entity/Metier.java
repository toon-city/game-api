package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Catalogue des métiers gérable depuis l'admin (Item l'est déjà de la même
 * façon — même mécanique CRUD, voir AdminItemService). Un métier rapporte
 * {@link #dailyPez} pezs par jour joué à celui qui l'exerce (payé par
 * UserActivityService.touchDailyActivity au même moment que l'incrément de
 * {@code User.daysPlayed}), et peut restreindre qui a le droit de le
 * choisir via {@link #minToonizLevel} / {@link #minDaysPlayed} — tous deux
 * {@code null} = pas de condition sur ce critère.
 */
@Entity
@Table(name = "metiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "daily_pez", nullable = false)
    @Builder.Default
    private int dailyPez = 0;

    /** Niveau tooniz minimum requis (0/1/2/3 — bronze/argent/or), ou null si aucune condition. */
    @Column(name = "min_tooniz_level")
    private Integer minToonizLevel;

    /** Nombre de jours joués minimum requis, ou null si aucune condition. */
    @Column(name = "min_days_played")
    private Integer minDaysPlayed;

    // ── Tenue de travail ───────────────────────────────────────────────────
    // Overlay appliqué par WorkOutfitService quand User.workOutfitActive est
    // vrai — tshirt/pant obligatoires côté formulaire admin (une tenue sans
    // les deux n'a pas de sens), hat optionnel. Nullable ici quand même :
    // les métiers créés avant cette colonne n'en ont pas encore.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_tshirt_item_id")
    private Item outfitTshirt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_pant_item_id")
    private Item outfitPant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_hat_item_id")
    private Item outfitHat;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
