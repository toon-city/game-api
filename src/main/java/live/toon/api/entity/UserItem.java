package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** true si le vêtement est actuellement porté par l'utilisateur. */
    @Column(nullable = false)
    @Builder.Default
    private boolean equipped = false;

    /**
     * Room dans laquelle ce meuble est posé.
     * null si l'item est dans l'inventaire (ni porté ni placé).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placed_in_room_id")
    private Room placedInRoom;

    @CreationTimestamp
    @Column(name = "acquired_at", updatable = false)
    private OffsetDateTime acquiredAt;
}
