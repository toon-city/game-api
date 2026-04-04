package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.AdminItemRequest;
import live.toon.api.dto.ItemDto;
import live.toon.api.entity.Item;
import live.toon.api.entity.ItemSubType;
import live.toon.api.entity.ItemType;
import live.toon.api.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminItemService {

    private static final int PAGE_SIZE = 30;

    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public Page<ItemDto> listItems(int page) {
        return itemRepository.findAll(PageRequest.of(page, PAGE_SIZE, Sort.by("id").descending()))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ItemDto getItem(Long id) {
        return toDto(requireItem(id));
    }

    @Transactional
    public ItemDto createItem(AdminItemRequest req) {
        Item item = Item.builder()
                .name(req.name())
                .itemType(ItemType.valueOf(req.itemType()))
                .subType(ItemSubType.valueOf(req.subType()))
                .possessable(req.possessable())
                .displayImage(req.displayImage())
                .spritePath(req.spritePath())
                .spriteKey(req.spriteKey())
                .build();
        return toDto(itemRepository.save(item));
    }

    @Transactional
    public ItemDto updateItem(Long id, AdminItemRequest req) {
        Item item = requireItem(id);
        if (req.name() != null)         item.setName(req.name());
        if (req.itemType() != null)     item.setItemType(ItemType.valueOf(req.itemType()));
        if (req.subType() != null)      item.setSubType(ItemSubType.valueOf(req.subType()));
        if (req.displayImage() != null) item.setDisplayImage(req.displayImage());
        if (req.spritePath() != null)   item.setSpritePath(req.spritePath());
        if (req.spriteKey() != null)    item.setSpriteKey(req.spriteKey());
        item.setPossessable(req.possessable());
        return toDto(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.delete(requireItem(id));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Item requireItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item introuvable : " + id));
    }

    private ItemDto toDto(Item i) {
        return ItemDto.builder()
                .id(i.getId())
                .name(i.getName())
                .itemType(i.getItemType().name())
                .subType(i.getSubType().name())
                .possessable(i.isPossessable())
                .displayImage(i.getDisplayImage())
                .spritePath(i.getSpritePath())
                .spriteKey(i.getSpriteKey())
                .build();
    }
}
