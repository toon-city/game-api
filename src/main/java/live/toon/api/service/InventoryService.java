package live.toon.api.service;

import live.toon.api.dto.ItemDto;
import live.toon.api.dto.UserItemDto;
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

        // Mettre à jour avatarOptions
        updateAvatarOptions(user);

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

        userItem.setEquipped(false);
        userItemRepository.save(userItem);
        updateAvatarOptions(user);

        return toDto(userItem);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Reconstruit le JSON avatarOptions à partir des items équipés,
     * puis sauvegarde en base.
     */
    void updateAvatarOptions(User user) {
        List<UserItem> equipped = userItemRepository.findAllEquipped(user);

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (UserItem ui : equipped) {
            String key = ui.getItem().getSpriteKey();
            String path = ui.getItem().getSpritePath();
            if (key != null && path != null) {
                if (!first) sb.append(",");
                sb.append("\"").append(key).append("\":\"").append(path).append("\"");
                first = false;
            }
        }
        sb.append("}");

        user.setAvatarOptionsJson(sb.toString());
        userRepository.save(user);
    }

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
