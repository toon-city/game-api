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

    /** Clé dans avatarOptions.clothing pour les vêtements (null pour les meubles). */
    @Column(name = "sprite_key", length = 64)
    private String spriteKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
