package live.toon.api.service;

import live.toon.api.dto.BuyOptionRequest.BuyOption;
import live.toon.api.dto.CollectionDto;
import live.toon.api.dto.ShopItemDto;
import live.toon.api.dto.UserItemDto;
import live.toon.api.entity.*;
import live.toon.api.repository.CollectionRepository;
import live.toon.api.repository.PurchaseLogRepository;
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
    private final PurchaseLogRepository purchaseLogRepository;

    // ─── Listing ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ShopItemDto> listItems(ShopId shopId, Long collectionId, ItemSubType subType, int page) {
        PageRequest pr = PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending());
        return shopItemRepository.findByShopIdAndAvailableTrue(shopId, collectionId, subType, pr).map(this::toDto);
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
    public UserItemDto buyItem(JwtPrincipal actor, Long shopItemId, BuyOption option, Integer quantityReq) {
        User user = userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        // Locked read: serializes concurrent buyers of the same item so the
        // stock check below is race-safe (see findByIdForUpdate's javadoc).
        ShopItem shopItem = shopItemRepository.findByIdForUpdate(shopItemId)
                .orElseThrow(() -> new IllegalArgumentException("Article introuvable en boutique"));

        if (!shopItem.isAvailable()) {
            throw new IllegalArgumentException("Cet article n'est plus disponible");
        }

        Item item = shopItem.getItem();

        // Non-possessable (coiffures, etc.) : un seul exemplaire "porté" a un
        // sens, en posséder plusieurs non — la quantité demandée est ignorée.
        int qty = (quantityReq == null || quantityReq < 1) ? 1 : quantityReq;
        if (!item.isPossessable()) qty = 1;

        if (shopItem.getStock() != null && shopItem.getStock() < qty) {
            throw new IllegalArgumentException(
                    shopItem.getStock() <= 0
                            ? "Cet article est épuisé"
                            : "Stock insuffisant (" + shopItem.getStock() + " disponible(s))");
        }

        // Débiter les fonds selon l'option choisie, pour la quantité totale
        deductFunds(user, shopItem, option, qty);

        // Décrémenter le stock si limité
        if (shopItem.getStock() != null) {
            shopItem.setStock(shopItem.getStock() - qty);
        }

        // Persist purchase log — un enregistrement par exemplaire (prix
        // unitaire), pas une ligne "quantité" agrégée : garde le même sens
        // pour tout code qui compte déjà les lignes de PurchaseLog (stats
        // admin) sans avoir besoin de connaître cette notion de quantité.
        int pezSpentEach   = (option == BuyOption.PEZ)   ? shopItem.getPezPrice()  : 0;
        int kredsSpentEach = (option == BuyOption.KREDS)  ? shopItem.getKredPrice() : shopItem.getKredBonus();
        for (int i = 0; i < qty; i++) {
            purchaseLogRepository.save(PurchaseLog.builder()
                    .userId(actor.getUserId())
                    .shopItem(shopItem)
                    .item(item)
                    .buyOption(option.name())
                    .pezSpent(pezSpentEach)
                    .kredsSpent(kredsSpentEach)
                    .build());
        }

        // Items non-possessables (coiffures) : marquer directement comme équipé dans user_items
        if (!item.isPossessable()) {
            // Déséquiper l'éventuel item du même sous-type déjà équipé
            List<UserItem> currentlyEquipped = userItemRepository.findEquippedBySubType(user, item.getSubType());
            currentlyEquipped.forEach(e -> e.setEquipped(false));
            userItemRepository.saveAll(currentlyEquipped);
            // Créer ou réutiliser une ligne user_items pour cet item
            UserItem existing = userItemRepository.findByUserAndItem(user, item).orElse(null);
            UserItem userItem;
            if (existing != null) {
                existing.setEquipped(true);
                userItem = userItemRepository.save(existing);
            } else {
                userItem = userItemRepository.save(UserItem.builder()
                        .user(user)
                        .item(item)
                        .equipped(true)
                        .build());
            }
            return toUserItemDto(userItem);
        }

        // Items possessables : une ligne user_items par exemplaire (chacun
        // individuellement équipable/plaçable, comme le reste du modèle).
        UserItem firstUserItem = null;
        for (int i = 0; i < qty; i++) {
            UserItem userItem = userItemRepository.save(UserItem.builder()
                    .user(user)
                    .item(item)
                    .build());
            if (firstUserItem == null) firstUserItem = userItem;
        }

        userRepository.save(user);
        return toUserItemDto(firstUserItem);
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    private void deductFunds(User user, ShopItem shopItem, BuyOption option, int qty) {
        switch (option) {
            case PEZ -> {
                if (shopItem.getPezPrice() == null) {
                    throw new IllegalArgumentException("Cet article n'est pas disponible à l'achat en pez");
                }
                int requiredPez = shopItem.getPezPrice() * qty;
                int requiredKreds = shopItem.getKredBonus() * qty;
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
                int requiredKreds = shopItem.getKredPrice() * qty;
                if (user.getKreds() < requiredKreds) {
                    throw new IllegalArgumentException("Kreds insuffisants");
                }
                user.setKreds(user.getKreds() - requiredKreds);
            }
        }
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
