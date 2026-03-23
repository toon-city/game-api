package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemDto {
    private Long id;
    private String name;
    /** FURNITURE, CLOTHING, MISC */
    private String itemType;
    /** FLOOR, WALL, WALLPAPER, PIECE, HAIRSTYLE, HAT, TOP, BOTTOM, MAKEUP, OTHER */
    private String subType;
    /** false = ne se possède pas (ex: coiffures — appliqué directement sur l'avatar) */
    private boolean possessable;
    /** Chemin relatif vers l'image d'affichage (boutique / inventaire) */
    private String displayImage;
    /** Chemin relatif vers le JSON de spritesheet (null si non applicable) */
    private String spritePath;
    /** Clé dans avatarOptions.clothing (null pour les meubles) */
    private String spriteKey;
}
