package live.toon.api.service;

import live.toon.api.dto.BuyOptionRequest.BuyOption;
import live.toon.api.dto.ShopItemDto;
import live.toon.api.dto.UserItemDto;
import live.toon.api.entity.*;
import live.toon.api.repository.CollectionRepository;
import live.toon.api.repository.ShopItemRepository;
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
class ShopServiceTest {

    @Mock UserRepository       userRepository;
    @Mock ShopItemRepository   shopItemRepository;
    @Mock UserItemRepository   userItemRepository;
    @Mock InventoryService     inventoryService;
    @Mock CollectionRepository collectionRepository;
    @InjectMocks ShopService   shopService;

    private static final UUID USER_ID  = UUID.randomUUID();
    private static final Long SHOP_ITEM_ID = 1L;

    private JwtPrincipal actor;
    private User         user;

    @BeforeEach
    void setUp() {
        actor = new JwtPrincipal(USER_ID, "alice", UserRank.ROLE_USER);
        user  = User.builder()
                .id(USER_ID).username("alice")
                .pez(500).kreds(100)
                .avatarOptionsJson("{}")
                .build();
    }

    // ── listItems ─────────────────────────────────────────────────────────────

    @Test
    void listItems_returnsAvailableItemsForShop() {
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.COUPE_TIFF, buildClothingItem(10L, ItemSubType.HAIRSTYLE));
        when(shopItemRepository.findByShopIdAndAvailableTrue(eq(ShopId.COUPE_TIFF), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(si)));

        Page<ShopItemDto> result = shopService.listItems(ShopId.COUPE_TIFF, null, 0);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(SHOP_ITEM_ID);
        verify(shopItemRepository, never())
                .findByShopIdAndAvailableTrueAndCollectionId(any(), any(), any());
    }

    @Test
    void listItems_filtersByCollection() {
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.COUPE_TIFF, buildClothingItem(10L, ItemSubType.HAIRSTYLE));
        when(shopItemRepository.findByShopIdAndAvailableTrueAndCollectionId(
                eq(ShopId.COUPE_TIFF), eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(si)));

        Page<ShopItemDto> result = shopService.listItems(ShopId.COUPE_TIFF, 1L, 0);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(shopItemRepository, never()).findByShopIdAndAvailableTrue(any(), any());
    }

    // ── buyItem : cas normaux ─────────────────────────────────────────────────

    @Test
    void buyItem_withPez_deductsPezAndCreatesUserItem() {
        Item item = buildClothingItem(10L, ItemSubType.TOP);
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, item);
        si.setPezPrice(100);
        si.setKredBonus(0);

        UserItem saved = UserItem.builder().id(20L).user(user).item(item).equipped(false).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));
        when(userItemRepository.save(any())).thenReturn(saved);
        when(userRepository.save(user)).thenReturn(user);

        UserItemDto result = shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ);

        assertThat(user.getPez()).isEqualTo(400);
        assertThat(result.getId()).isEqualTo(20L);
    }

    @Test
    void buyItem_withPezAndKredBonus_deductsBoth() {
        Item item = buildClothingItem(10L, ItemSubType.TOP);
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, item);
        si.setPezPrice(100);
        si.setKredBonus(20);

        UserItem saved = UserItem.builder().id(20L).user(user).item(item).equipped(false).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));
        when(userItemRepository.save(any())).thenReturn(saved);
        when(userRepository.save(user)).thenReturn(user);

        shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ);

        assertThat(user.getPez()).isEqualTo(400);
        assertThat(user.getKreds()).isEqualTo(80);
    }

    @Test
    void buyItem_withKreds_deductsKredsOnly() {
        Item item = buildClothingItem(10L, ItemSubType.TOP);
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, item);
        si.setKredPrice(30);

        UserItem saved = UserItem.builder().id(20L).user(user).item(item).equipped(false).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));
        when(userItemRepository.save(any())).thenReturn(saved);
        when(userRepository.save(user)).thenReturn(user);

        shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.KREDS);

        assertThat(user.getKreds()).isEqualTo(70);
        assertThat(user.getPez()).isEqualTo(500); // inchangé
    }

    @Test
    void buyItem_notPossessable_updatesAvatarOptionsAndReturnsNullId() {
        Item item = Item.builder()
                .id(10L).name("Cheveux courts").itemType(ItemType.CLOTHING)
                .subType(ItemSubType.HAIRSTYLE).possessable(false)
                .displayImage("img/hair.png").spritePath("assets/hair.json")
                .spriteKey("hair").build();

        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.COUPE_TIFF, item);
        si.setPezPrice(50);
        si.setKredBonus(0);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));
        when(userRepository.save(user)).thenReturn(user);

        UserItemDto result = shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ);

        assertThat(result.getId()).isNull();
        assertThat(result.isEquipped()).isTrue();
        verify(userItemRepository, never()).save(any());
    }

    @Test
    void buyItem_notPossessable_replacesExistingKeyInAvatarOptions() {
        user.setAvatarOptionsJson("{\"hair\":\"assets/old_hair.json\"}");

        Item item = Item.builder()
                .id(10L).name("Nouveau look").itemType(ItemType.CLOTHING)
                .subType(ItemSubType.HAIRSTYLE).possessable(false)
                .displayImage("img/new_hair.png").spritePath("assets/new_hair.json")
                .spriteKey("hair").build();

        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.COUPE_TIFF, item);
        si.setPezPrice(50);
        si.setKredBonus(0);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));
        when(userRepository.save(user)).thenReturn(user);

        shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ);

        assertThat(user.getAvatarOptionsJson())
                .contains("\"hair\":\"assets/new_hair.json\"")
                .doesNotContain("old_hair");
    }

    // ── buyItem : cas d'erreur ─────────────────────────────────────────────────

    @Test
    void buyItem_throwsWhenPezInsufficient() {
        user.setPez(50); // insuffisant pour pezPrice=100
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, buildClothingItem(10L, ItemSubType.TOP));
        si.setPezPrice(100);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));

        assertThatThrownBy(() -> shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pez insuffisants");
    }

    @Test
    void buyItem_throwsWhenKredsInsufficientForBonus() {
        user.setKreds(5); // insuffisant pour kredBonus=20
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, buildClothingItem(10L, ItemSubType.TOP));
        si.setPezPrice(100);
        si.setKredBonus(20);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));

        assertThatThrownBy(() -> shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kreds insuffisants");
    }

    @Test
    void buyItem_throwsWhenKredsInsufficientForKredsPurchase() {
        user.setKreds(10); // insuffisant pour kredPrice=30
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, buildClothingItem(10L, ItemSubType.TOP));
        si.setKredPrice(30);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));

        assertThatThrownBy(() -> shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.KREDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kreds insuffisants");
    }

    @Test
    void buyItem_throwsWhenPezOptionNotAvailable() {
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, buildClothingItem(10L, ItemSubType.TOP));
        si.setPezPrice(null); // pas de prix en pez

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));

        assertThatThrownBy(() -> shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pas disponible à l'achat en pez");
    }

    @Test
    void buyItem_throwsWhenKredsOptionNotAvailable() {
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, buildClothingItem(10L, ItemSubType.TOP));
        si.setKredPrice(null); // pas de prix en kreds

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));

        assertThatThrownBy(() -> shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.KREDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pas disponible à l'achat en kreds");
    }

    @Test
    void buyItem_throwsWhenShopItemNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Article introuvable");
    }

    @Test
    void buyItem_throwsWhenItemUnavailable() {
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, buildClothingItem(10L, ItemSubType.TOP));
        si.setAvailable(false);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));

        assertThatThrownBy(() -> shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("n'est plus disponible");
    }

    @Test
    void buyItem_throwsWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    // ── buyItem : stock ───────────────────────────────────────────────────────

    @Test
    void buyItem_throwsWhenOutOfStock() {
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, buildClothingItem(10L, ItemSubType.TOP));
        si.setPezPrice(100);
        si.setStock(0);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));

        assertThatThrownBy(() -> shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("épuisé");
    }

    @Test
    void buyItem_decrementsStockWhenLimited() {
        Item item = buildClothingItem(10L, ItemSubType.TOP);
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, item);
        si.setPezPrice(100);
        si.setKredBonus(0);
        si.setStock(5);

        UserItem saved = UserItem.builder().id(20L).user(user).item(item).equipped(false).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));
        when(userItemRepository.save(any())).thenReturn(saved);
        when(userRepository.save(user)).thenReturn(user);

        shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ);

        assertThat(si.getStock()).isEqualTo(4);
    }

    @Test
    void buyItem_doesNotDecrementStockWhenUnlimited() {
        Item item = buildClothingItem(10L, ItemSubType.TOP);
        ShopItem si = buildShopItem(SHOP_ITEM_ID, ShopId.IKEBO, item);
        si.setPezPrice(100);
        si.setKredBonus(0);
        // stock null = illimité

        UserItem saved = UserItem.builder().id(20L).user(user).item(item).equipped(false).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shopItemRepository.findById(SHOP_ITEM_ID)).thenReturn(Optional.of(si));
        when(userItemRepository.save(any())).thenReturn(saved);
        when(userRepository.save(user)).thenReturn(user);

        shopService.buyItem(actor, SHOP_ITEM_ID, BuyOption.PEZ);

        assertThat(si.getStock()).isNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Item buildClothingItem(Long id, ItemSubType subType) {
        return Item.builder()
                .id(id).name("Item " + id)
                .itemType(ItemType.CLOTHING).subType(subType)
                .possessable(true).displayImage("img/item" + id + ".png")
                .spritePath("assets/item" + id + ".json").spriteKey("key_" + id)
                .build();
    }

    private ShopItem buildShopItem(Long id, ShopId shopId, Item item) {
        return ShopItem.builder()
                .id(id).shopId(shopId).item(item)
                .pezPrice(200).kredBonus(0).kredPrice(50)
                .available(true)
                .build();
    }
}
