package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.*;
import live.toon.api.entity.*;
import live.toon.api.repository.CollectionRepository;
import live.toon.api.repository.ItemRepository;
import live.toon.api.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminShopService {

    private static final int PAGE_SIZE = 30;

    private final ShopItemRepository shopItemRepository;
    private final CollectionRepository collectionRepository;
    private final ItemRepository itemRepository;

    // ─── Shop Items ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ShopItemDto> listShopItems(ShopId shopId, int page) {
        return shopItemRepository.findAllByShopIdOrderById(shopId,
                PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending()))
                .map(this::toShopItemDto);
    }

    @Transactional
    public ShopItemDto createShopItem(ShopId shopId, AdminShopItemRequest req) {
        Item item = itemRepository.findById(req.itemId())
                .orElseThrow(() -> new EntityNotFoundException("Item introuvable : " + req.itemId()));
        ItemCollection collection = req.collectionId() != null
                ? collectionRepository.findById(req.collectionId())
                        .orElseThrow(() -> new EntityNotFoundException("Collection introuvable"))
                : null;
        ShopItem si = ShopItem.builder()
                .shopId(shopId)
                .item(item)
                .pezPrice(req.pezPrice())
                .kredBonus(req.kredBonus() != null ? req.kredBonus() : 0)
                .kredPrice(req.kredPrice())
                .available(req.available() != null ? req.available() : true)
                .stock(req.stock())
                .collection(collection)
                .build();
        return toShopItemDto(shopItemRepository.save(si));
    }

    @Transactional
    public ShopItemDto updateShopItem(Long id, AdminShopItemRequest req) {
        ShopItem si = shopItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ShopItem introuvable : " + id));
        if (req.pezPrice() != null)  si.setPezPrice(req.pezPrice());
        if (req.kredBonus() != null) si.setKredBonus(req.kredBonus());
        if (req.kredPrice() != null) si.setKredPrice(req.kredPrice());
        if (req.available() != null) si.setAvailable(req.available());
        if (req.stock() != null)     si.setStock(req.stock());
        if (req.collectionId() != null) {
            ItemCollection col = collectionRepository.findById(req.collectionId())
                    .orElseThrow(() -> new EntityNotFoundException("Collection introuvable"));
            si.setCollection(col);
        }
        return toShopItemDto(shopItemRepository.save(si));
    }

    @Transactional
    public void deleteShopItem(Long id) {
        shopItemRepository.deleteById(id);
    }

    // ─── Collections ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CollectionDto> listCollections(ShopId shopId) {
        return collectionRepository.findAllByShopIdOrderBySortOrderAsc(shopId)
                .stream().map(this::toCollectionDto).toList();
    }

    @Transactional
    public CollectionDto createCollection(ShopId shopId, AdminCollectionRequest req) {
        ItemCollection col = ItemCollection.builder()
                .shopId(shopId)
                .name(req.name())
                .bannerImage(req.bannerImage())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .enabled(req.enabled() != null ? req.enabled() : true)
                .build();
        return toCollectionDto(collectionRepository.save(col));
    }

    @Transactional
    public CollectionDto updateCollection(Long id, AdminCollectionRequest req) {
        ItemCollection col = collectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Collection introuvable : " + id));
        if (req.name() != null)        col.setName(req.name());
        if (req.bannerImage() != null) col.setBannerImage(req.bannerImage());
        if (req.sortOrder() != null)   col.setSortOrder(req.sortOrder());
        if (req.enabled() != null)     col.setEnabled(req.enabled());
        return toCollectionDto(collectionRepository.save(col));
    }

    @Transactional
    public void deleteCollection(Long id) {
        collectionRepository.deleteById(id);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ShopItemDto toShopItemDto(ShopItem si) {
        return ShopItemDto.builder()
                .id(si.getId())
                .item(ItemDto.builder()
                        .id(si.getItem().getId())
                        .name(si.getItem().getName())
                        .itemType(si.getItem().getItemType().name())
                        .subType(si.getItem().getSubType().name())
                        .possessable(si.getItem().isPossessable())
                        .displayImage(si.getItem().getDisplayImage())
                        .spritePath(si.getItem().getSpritePath())
                        .spriteKey(si.getItem().getSpriteKey())
                        .build())
                .pezPrice(si.getPezPrice())
                .kredBonus(si.getKredBonus())
                .kredPrice(si.getKredPrice())
                .collectionId(si.getCollection() != null ? si.getCollection().getId() : null)
                .stock(si.getStock())
                .build();
    }

    private CollectionDto toCollectionDto(ItemCollection c) {
        return CollectionDto.builder()
                .id(c.getId())
                .shopId(c.getShopId().name())
                .name(c.getName())
                .bannerImage(c.getBannerImage())
                .sortOrder(c.getSortOrder())
                .enabled(c.isEnabled())
                .build();
    }
}
