package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopItemDto {
    private Long id;
    private ItemDto item;
    /**
     * Prix en pez (null = pas d'option pez).
     * Si kredBonus > 0, le joueur paye pezPrice pez ET kredBonus kreds.
     */
    private Integer pezPrice;
    /** Kreds supplémentaires requis en plus de pezPrice (0 = pez seul). */
    private int kredBonus;
    /** Prix en kreds seuls (null = pas d'option kreds). */
    private Integer kredPrice;
    /** Identifiant de la collection à laquelle appartient cet article (null = aucune). */
    private Long collectionId;
    /** Stock restant (null = illimité, 0 = épuisé). */
    private Integer stock;
}
