package live.toon.api.service;

import live.toon.api.entity.Item;
import live.toon.api.entity.Metier;
import live.toon.api.entity.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies the "tenue de travail" overlay to a clothing map — shared by
 * InventoryService (own-user badge/room appearance) and UserController
 * (viewing someone else's profile). Both need the exact same rule, so it
 * lives here instead of being duplicated inline in each.
 *
 * NOT a simple overlay of tshirt/pant/hat on top of whatever's equipped:
 * per spec, when active the user keeps ONLY their skin color and hair
 * (cut — there's no separate hair-color system in this codebase to keep
 * distinct from it) — everything else (makeup, ring, and tshirt/pant/hat)
 * is replaced by the métier's own outfit, including categories the
 * outfit doesn't define (no hat on the outfit = no hat shown at all, not
 * "keep whatever hat was equipped"). The equipped items themselves are
 * never touched — this only reshapes the map handed to the renderer.
 */
@Service
public class WorkOutfitService {

    private static final String HAIR = "hair";

    public Map<String, String> applyOverlay(User user, Map<String, String> equippedClothing) {
        Metier metier = user.getMetier();
        if (!user.isWorkOutfitActive() || metier == null) {
            return equippedClothing;
        }

        Map<String, String> result = new HashMap<>();
        String hair = equippedClothing.get(HAIR);
        if (hair != null) {
            result.put(HAIR, hair);
        }
        putIfSprited(result, metier.getOutfitTshirt());
        putIfSprited(result, metier.getOutfitPant());
        putIfSprited(result, metier.getOutfitHat());
        return result;
    }

    private void putIfSprited(Map<String, String> map, Item item) {
        if (item != null && item.getSpriteKey() != null && item.getSpritePath() != null) {
            map.put(item.getSpriteKey(), item.getSpritePath());
        }
    }
}
