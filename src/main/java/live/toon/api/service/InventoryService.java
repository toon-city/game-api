package live.toon.api.service;

import live.toon.api.dto.ItemDto;
import live.toon.api.dto.UserItemDto;
import live.toon.api.entity.ItemSubType;
import live.toon.api.entity.ItemType;
import live.toon.api.entity.User;
import live.toon.api.entity.UserItem;
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
public class InventoryService {

    private static final int PAGE_SIZE = 48;

    private final UserRepository userRepository;
    private final UserItemRepository userItemRepository;
    private final WorkOutfitService workOutfitService;

    // ─── Listing ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UserItemDto> listItems(JwtPrincipal actor, ItemType itemType, int page) {
        User user = loadUser(actor);
        PageRequest pr = PageRequest.of(page, PAGE_SIZE, Sort.by("acquiredAt").descending());

        Page<UserItem> result = (itemType == null)
                ? userItemRepository.findInventory(user, pr)
                : userItemRepository.findInventoryByType(user, itemType, pr);

        return result.map(this::toDto);
    }

    /**
     * Vêtements actuellement équipés, sous la forme spriteKey → spritePath —
     * même format que game-server-java's RoomStateService.buildClothingMap(),
     * pour que n'importe quel client puisse habiller un Avatar avec
     * `changeClothing(spriteKey, spritePath)` sans repasser par une room.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, String> getEquippedClothing(JwtPrincipal actor) {
        User user = loadUser(actor);
        var equipped = userItemRepository.findAllEquipped(user).stream()
                .map(UserItem::getItem)
                .filter(item -> item.getSpriteKey() != null && item.getSpritePath() != null)
                .collect(java.util.stream.Collectors.toMap(
                        live.toon.api.entity.Item::getSpriteKey,
                        live.toon.api.entity.Item::getSpritePath,
                        (a, b) -> a));
        return workOutfitService.applyOverlay(user, equipped);
    }

    // ─── Équipement ───────────────────────────────────────────────────────────

    @Transactional
    public UserItemDto equipItem(JwtPrincipal actor, Long userItemId) {
        User user = loadUser(actor);
        UserItem userItem = userItemRepository.findByIdAndUser(userItemId, user)
                .orElseThrow(() -> new IllegalArgumentException("Objet introuvable dans votre inventaire"));

        if (!userItem.getItem().getItemType().equals(live.toon.api.entity.ItemType.CLOTHING)) {
            throw new IllegalArgumentException("Seuls les vêtements peuvent être équipés");
        }

        // Déséquiper l'éventuel item du même sous-type déjà équipé
        List<UserItem> alreadyEquipped = userItemRepository.findEquippedBySubType(user, userItem.getItem().getSubType());
        alreadyEquipped.forEach(e -> e.setEquipped(false));
        userItemRepository.saveAll(alreadyEquipped);

        // Équiper le nouvel item
        userItem.setEquipped(true);
        userItemRepository.save(userItem);

        return toDto(userItem);
    }

    @Transactional
    public UserItemDto unequipItem(JwtPrincipal actor, Long userItemId) {
        User user = loadUser(actor);
        UserItem userItem = userItemRepository.findByIdAndUser(userItemId, user)
                .orElseThrow(() -> new IllegalArgumentException("Objet introuvable dans votre inventaire"));

        if (!userItem.isEquipped()) {
            throw new IllegalArgumentException("Cet item n'est pas équipé");
        }

        // TOP/BOTTOM sont toujours équipés — retirer sans remplacer laisserait
        // l'avatar torse nu/sans bas. Switching (equipItem sur un autre TOP/
        // BOTTOM) reste permis : ça déséquipe l'ancien automatiquement, ce
        // n'est jamais un retrait pur.
        ItemSubType subType = userItem.getItem().getSubType();
        if (subType == ItemSubType.TOP || subType == ItemSubType.BOTTOM) {
            throw new IllegalArgumentException("Ce vêtement ne peut pas être retiré — équipez-en un autre à la place.");
        }

        userItem.setEquipped(false);
        userItemRepository.save(userItem);

        return toDto(userItem);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private User loadUser(JwtPrincipal actor) {
    return userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    public static ItemDto toItemDto(live.toon.api.entity.Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .itemType(item.getItemType().name())
                .subType(item.getSubType().name())
                .possessable(item.isPossessable())
                .displayImage(item.getDisplayImage())
                .spritePath(item.getSpritePath())
                .spriteKey(item.getSpriteKey())
                .build();
    }

    private UserItemDto toDto(UserItem ui) {
        return UserItemDto.builder()
                .id(ui.getId())
                .item(toItemDto(ui.getItem()))
                .equipped(ui.isEquipped())
                .placedInRoomId(ui.getPlacedInRoom() != null ? ui.getPlacedInRoom().getId() : null)
                .acquiredAt(ui.getAcquiredAt())
                .build();
    }
}
