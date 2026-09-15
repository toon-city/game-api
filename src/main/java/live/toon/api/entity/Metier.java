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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
