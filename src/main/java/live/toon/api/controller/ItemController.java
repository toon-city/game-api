package live.toon.api.controller;

import live.toon.api.dto.ItemDto;
import live.toon.api.entity.ItemType;
import live.toon.api.repository.ItemRepository;
import live.toon.api.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Public (authenticated, non-admin) item catalogue search — needed for the
 * trade center's "quel objet je veux recevoir" picker, which has to search
 * the whole catalogue, not just what I own. AdminItemController is
 * ROLE_ADMIN-gated and /api/shops/*&#47;items is scoped to one shop, neither
 * fits; reuses ItemRepository.findByNameContainingIgnoreCase (already
 * there for the admin catalogue) behind a plain authenticated route.
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ItemController {

    private static final int PAGE_SIZE = 20;

    private final ItemRepository itemRepository;

    @GetMapping
    public ResponseEntity<Page<ItemDto>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ItemType itemType) {
        var pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("name").ascending());
        String q = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<live.toon.api.entity.Item> page;
        if (q != null && itemType != null) {
            page = itemRepository.findByNameContainingIgnoreCaseAndItemType(q, itemType, pageable);
        } else if (q != null) {
            page = itemRepository.findByNameContainingIgnoreCase(q, pageable);
        } else if (itemType != null) {
            page = itemRepository.findByItemType(itemType, pageable);
        } else {
            page = itemRepository.findAll(pageable);
        }

        return ResponseEntity.ok(page.map(InventoryService::toItemDto));
    }
}
