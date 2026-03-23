package live.toon.api.service;

import live.toon.api.dto.ItemDto;
import live.toon.api.dto.UserItemDto;
import live.toon.api.entity.*;
import live.toon.api.repository.UserItemRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.UserRank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserItemRepository userItemRepository;
    @InjectMocks InventoryService inventoryService;

    private static final UUID USER_ID = UUID.randomUUID();
    private JwtPrincipal actor;
    private User user;

    @BeforeEach
    void setUp() {
        actor = new JwtPrincipal(USER_ID, "alice", UserRank.ROLE_USER);
        user  = User.builder().id(USER_ID).username("alice").pez(1500).kreds(50).build();
    }

    // ── listItems ─────────────────────────────────────────────────────────────

    @Test
    void listItems_returnsAllItems_whenNoTypeFilter() {
        UserItem ui = buildUserItem(10L, buildClothingItem(1L, ItemSubType.TOP), false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findInventory(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ui)));

        Page<UserItemDto> result = inventoryService.listItems(actor, null, 0);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
        verify(userItemRepository, never()).findInventoryByType(any(), any(), any());
    }

    @Test
    void listItems_filtersByItemType() {
        UserItem ui = buildUserItem(10L, buildClothingItem(1L, ItemSubType.TOP), false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findInventoryByType(eq(user), eq(ItemType.CLOTHING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ui)));

        Page<UserItemDto> result = inventoryService.listItems(actor, ItemType.CLOTHING, 0);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userItemRepository, never()).findInventory(any(), any());
    }

    @Test
    void listItems_throwsWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.listItems(actor, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    @Test
    void listItems_returnsEmptyPageWhenInventoryEmpty() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findInventory(eq(user), any(Pageable.class))).thenReturn(Page.empty());

        Page<UserItemDto> result = inventoryService.listItems(actor, null, 0);

        assertThat(result.isEmpty()).isTrue();
    }

    // ── equipItem ─────────────────────────────────────────────────────────────

    @Test
    void equipItem_setsEquippedAndUpdatesAvatar() {
        Item item = buildClothingItem(1L, ItemSubType.TOP);
        UserItem ui = buildUserItem(10L, item, false);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(ui));
        when(userItemRepository.findEquippedBySubType(user, ItemSubType.TOP)).thenReturn(List.of());
        when(userItemRepository.save(ui)).thenReturn(ui);
        when(userItemRepository.findAllEquipped(user)).thenReturn(List.of(ui));
        when(userRepository.save(user)).thenReturn(user);

        UserItemDto result = inventoryService.equipItem(actor, 10L);

        assertThat(ui.isEquipped()).isTrue();
        assertThat(result.isEquipped()).isTrue();
        verify(userItemRepository).save(ui);
        verify(userRepository).save(user);
    }

    @Test
    void equipItem_desequipsPreviousItemOfSameSubType() {
        Item newItem = buildClothingItem(1L, ItemSubType.TOP);
        Item oldItem = buildClothingItem(2L, ItemSubType.TOP);
        UserItem newUi = buildUserItem(10L, newItem, false);
        UserItem oldUi = buildUserItem(11L, oldItem, true);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(newUi));
        when(userItemRepository.findEquippedBySubType(user, ItemSubType.TOP)).thenReturn(List.of(oldUi));
        when(userItemRepository.save(newUi)).thenReturn(newUi);
        when(userItemRepository.findAllEquipped(user)).thenReturn(List.of(newUi));
        when(userRepository.save(user)).thenReturn(user);

        inventoryService.equipItem(actor, 10L);

        assertThat(oldUi.isEquipped()).isFalse();
        verify(userItemRepository).saveAll(List.of(oldUi));
    }

    @Test
    void equipItem_throwsWhenItemNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.equipItem(actor, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void equipItem_throwsWhenItemIsNotClothing() {
        Item furniture = Item.builder()
                .id(1L).name("Table").itemType(ItemType.FURNITURE).subType(ItemSubType.PIECE)
                .possessable(true).displayImage("img.png").build();
        UserItem ui = buildUserItem(10L, furniture, false);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(ui));

        assertThatThrownBy(() -> inventoryService.equipItem(actor, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vêtements");
    }

    // ── unequipItem ───────────────────────────────────────────────────────────

    @Test
    void unequipItem_setsEquippedFalse() {
        Item item = buildClothingItem(1L, ItemSubType.TOP);
        UserItem ui = buildUserItem(10L, item, true);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(ui));
        when(userItemRepository.save(ui)).thenReturn(ui);
        when(userItemRepository.findAllEquipped(user)).thenReturn(List.of());
        when(userRepository.save(user)).thenReturn(user);

        UserItemDto result = inventoryService.unequipItem(actor, 10L);

        assertThat(ui.isEquipped()).isFalse();
        assertThat(result.isEquipped()).isFalse();
    }

    @Test
    void unequipItem_throwsWhenItemNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.unequipItem(actor, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void unequipItem_throwsWhenItemAlreadyUnequipped() {
        Item item = buildClothingItem(1L, ItemSubType.TOP);
        UserItem ui = buildUserItem(10L, item, false);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userItemRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(ui));

        assertThatThrownBy(() -> inventoryService.unequipItem(actor, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("n'est pas équipé");
    }

    // ── updateAvatarOptions ───────────────────────────────────────────────────

    @Test
    void updateAvatarOptions_buildsJsonFromEquippedItems() {
        Item item = buildClothingItem(1L, ItemSubType.TOP);
        item.setSpriteKey("top");
        item.setSpritePath("assets/clothes/shirt1.json");
        UserItem ui = buildUserItem(10L, item, true);

        when(userItemRepository.findAllEquipped(user)).thenReturn(List.of(ui));
        when(userRepository.save(user)).thenReturn(user);

        inventoryService.updateAvatarOptions(user);

        assertThat(user.getAvatarOptionsJson())
                .isEqualTo("{\"top\":\"assets/clothes/shirt1.json\"}");
    }

    @Test
    void updateAvatarOptions_buildsEmptyJsonWhenNoEquippedItems() {
        when(userItemRepository.findAllEquipped(user)).thenReturn(List.of());
        when(userRepository.save(user)).thenReturn(user);

        inventoryService.updateAvatarOptions(user);

        assertThat(user.getAvatarOptionsJson()).isEqualTo("{}");
    }

    @Test
    void updateAvatarOptions_skipsItemsWithNullSpriteKey() {
        Item item = buildClothingItem(1L, ItemSubType.TOP);
        item.setSpriteKey(null);
        UserItem ui = buildUserItem(10L, item, true);

        when(userItemRepository.findAllEquipped(user)).thenReturn(List.of(ui));
        when(userRepository.save(user)).thenReturn(user);

        inventoryService.updateAvatarOptions(user);

        assertThat(user.getAvatarOptionsJson()).isEqualTo("{}");
    }

    // ── toItemDto (méthode statique publique) ─────────────────────────────────

    @Test
    void toItemDto_mapsAllFields() {
        Item item = buildClothingItem(5L, ItemSubType.HAT);
        item.setName("Chapeau de Pâques");
        item.setDisplayImage("img/hat.png");
        item.setSpritePath("assets/hat.json");
        item.setSpriteKey("hat");

        ItemDto dto = InventoryService.toItemDto(item);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getName()).isEqualTo("Chapeau de Pâques");
        assertThat(dto.getItemType()).isEqualTo("CLOTHING");
        assertThat(dto.getSubType()).isEqualTo("HAT");
        assertThat(dto.isPossessable()).isTrue();
        assertThat(dto.getDisplayImage()).isEqualTo("img/hat.png");
        assertThat(dto.getSpritePath()).isEqualTo("assets/hat.json");
        assertThat(dto.getSpriteKey()).isEqualTo("hat");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Item buildClothingItem(Long id, ItemSubType subType) {
        return Item.builder()
                .id(id)
                .name("Item " + id)
                .itemType(ItemType.CLOTHING)
                .subType(subType)
                .possessable(true)
                .displayImage("img/item" + id + ".png")
                .spritePath("assets/item" + id + ".json")
                .spriteKey("key_" + id)
                .build();
    }

    private UserItem buildUserItem(Long id, Item item, boolean equipped) {
        return UserItem.builder()
                .id(id)
                .user(user)
                .item(item)
                .equipped(equipped)
                .build();
    }
}
