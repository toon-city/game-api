package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_type", nullable = false, length = 30)
    private ItemSubType subType;

    /** false = n'est pas conservé dans l'inventaire (ex: coiffures). */
    @Column(nullable = false)
    @Builder.Default
    private boolean possessable = true;

    /** Chemin relatif vers l'image d'affichage (boutique / inventaire). */
    @Column(name = "display_image", nullable = false, length = 255)
    private String displayImage;

    /** Chemin relatif vers le fichier JSON de spritesheet (null si non nécessaire). */
    @Column(name = "sprite_path", length = 255)
    private String spritePath;

    /**
     * Old game's STYPE (18 = blocking furniture, 19 = wall-mounted decor,
     * 20 = floor decal) — game-core's HouseView.checkCollision/FurnitureView
     * only treat 18 as a real obstacle with avatar-priority depth; 19/20
     * are non-blocking and sort at the lower floor layer. 18 by default for
     * every item outside the 4 recovered public-room categories.
     */
    @Column(name = "render_type", nullable = false)
    @Builder.Default
    private int renderType = 18;

    /** Clé dans avatarOptions.clothing pour les vêtements (null pour les meubles). */
    @Column(name = "sprite_key", length = 64)
    private String spriteKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
