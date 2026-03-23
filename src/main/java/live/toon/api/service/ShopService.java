package live.toon.api.service;

import live.toon.api.dto.BuyOptionRequest.BuyOption;
import live.toon.api.dto.CollectionDto;
import live.toon.api.dto.ShopItemDto;
import live.toon.api.dto.UserItemDto;
import live.toon.api.entity.*;
import live.toon.api.repository.CollectionRepository;
import live.toon.api.repository.ShopItemRepository;
import live.toon.api.repository.UserItemRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {

    private static final int PAGE_SIZE = 20;

    private final UserRepository userRepository;
    private final ShopItemRepository shopItemRepository;
    private final UserItemRepository userItemRepository;
    private final InventoryService inventoryService;
    private final CollectionRepository collectionRepository;

    // ─── Listing ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ShopItemDto> listItems(ShopId shopId, Long collectionId, int page) {
        PageRequest pr = PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending());

        Page<ShopItem> result = (collectionId == null)
                ? shopItemRepository.findByShopIdAndAvailableTrue(shopId, pr)
                : shopItemRepository.findByShopIdAndAvailableTrueAndCollectionId(shopId, collectionId, pr);

        return result.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CollectionDto> listCollections(ShopId shopId) {
        return collectionRepository.findByShopIdAndEnabledTrueOrderBySortOrderAsc(shopId).stream()
                .map(c -> CollectionDto.builder()
                        .id(c.getId())
                        .shopId(c.getShopId().name())
                        .name(c.getName())
                        .bannerImage(c.getBannerImage())
                        .sortOrder(c.getSortOrder())
                        .enabled(c.isEnabled())
                        .build())
                .toList();
    }

    // ─── Achat ────────────────────────────────────────────────────────────────

    @Transactional
    public UserItemDto buyItem(JwtPrincipal actor, Long shopItemId, BuyOption option) {
        User user = userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        ShopItem shopItem = shopItemRepository.findById(shopItemId)
                .orElseThrow(() -> new IllegalArgumentException("Article introuvable en boutique"));

        if (!shopItem.isAvailable()) {
            throw new IllegalArgumentException("Cet article n'est plus disponible");
        }

        if (shopItem.getStock() != null && shopItem.getStock() <= 0) {
            throw new IllegalArgumentException("Cet article est épuisé");
        }

        // Débiter les fonds selon l'option choisie
        deductFunds(user, shopItem, option);

        // Décrémenter le stock si limité
        if (shopItem.getStock() != null) {
            shopItem.setStock(shopItem.getStock() - 1);
        }

        Item item = shopItem.getItem();

        // Items non-possessables (coiffures) : on met à jour l'avatar directement
        if (!item.isPossessable()) {
            applyNonPossessableItem(user, item);
            userRepository.save(user);
            // Retourner un DTO "virtuel" sans ligne user_items
            return UserItemDto.builder()
                    .id(null)
                    .item(InventoryService.toItemDto(item))
                    .equipped(true)
                    .acquiredAt(null)
                    .build();
        }

        // Items possessables : créer la ligne user_items
        UserItem userItem = userItemRepository.save(UserItem.builder()
                .user(user)
                .item(item)
                .build());

        userRepository.save(user);
        return toUserItemDto(userItem);
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    private void deductFunds(User user, ShopItem shopItem, BuyOption option) {
        switch (option) {
            case PEZ -> {
                if (shopItem.getPezPrice() == null) {
                    throw new IllegalArgumentException("Cet article n'est pas disponible à l'achat en pez");
                }
                int requiredPez = shopItem.getPezPrice();
                int requiredKreds = shopItem.getKredBonus();
                if (user.getPez() < requiredPez) {
                    throw new IllegalArgumentException("Pez insuffisants");
                }
                if (user.getKreds() < requiredKreds) {
                    throw new IllegalArgumentException("Kreds insuffisants");
                }
                user.setPez(user.getPez() - requiredPez);
                user.setKreds(user.getKreds() - requiredKreds);
            }
            case KREDS -> {
                if (shopItem.getKredPrice() == null) {
                    throw new IllegalArgumentException("Cet article n'est pas disponible à l'achat en kreds");
                }
                if (user.getKreds() < shopItem.getKredPrice()) {
                    throw new IllegalArgumentException("Kreds insuffisants");
                }
                user.setKreds(user.getKreds() - shopItem.getKredPrice());
            }
        }
    }

    /**
     * Pour un item non-possessable (ex: coiffure), on met directement à jour
     * la clé correspondante dans avatarOptions du joueur.
     */
    private void applyNonPossessableItem(User user, Item item) {
        if (item.getSpriteKey() == null || item.getSpritePath() == null) return;

        String current = user.getAvatarOptionsJson();
        if (current == null || current.isBlank()) current = "{}";

        // Injection simple de la clé dans le JSON (sans dépendance Jackson)
        String key = item.getSpriteKey();
        String value = item.getSpritePath();
        String entry = "\"" + key + "\":\"" + value + "\"";

        // Supprime l'ancienne valeur de la clé si elle existe
        String updated = current.replaceFirst("\"" + key + "\":\\s*\"[^\"]*\"", entry);
        if (!updated.contains("\"" + key + "\"")) {
            // Clé absente → l'insérer
            updated = current.equals("{}")
                    ? "{" + entry + "}"
                    : current.substring(0, current.length() - 1) + "," + entry + "}";
        }
        user.setAvatarOptionsJson(updated);
    }

    private ShopItemDto toDto(ShopItem si) {
        return ShopItemDto.builder()
                .id(si.getId())
                .item(InventoryService.toItemDto(si.getItem()))
                .pezPrice(si.getPezPrice())
                .kredBonus(si.getKredBonus())
                .kredPrice(si.getKredPrice())
                .collectionId(si.getCollection() != null ? si.getCollection().getId() : null)
                .stock(si.getStock())
                .build();
    }

    private UserItemDto toUserItemDto(UserItem ui) {
        return UserItemDto.builder()
                .id(ui.getId())
                .item(InventoryService.toItemDto(ui.getItem()))
                .equipped(ui.isEquipped())
                .placedInRoomId(null)
                .acquiredAt(ui.getAcquiredAt())
                .build();
    }
}
