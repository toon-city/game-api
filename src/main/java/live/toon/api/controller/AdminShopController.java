package live.toon.api.controller;

import live.toon.api.dto.*;
import live.toon.api.entity.ShopId;
import live.toon.api.service.AdminShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminShopController {

    private final AdminShopService adminShopService;

    // ─── Shop Items ────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/shops/{shopId}/items")
    public ResponseEntity<Page<ShopItemDto>> listItems(
            @PathVariable ShopId shopId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(adminShopService.listShopItems(shopId, page));
    }

    @PostMapping("/api/admin/shops/{shopId}/items")
    public ResponseEntity<ShopItemDto> createItem(
            @PathVariable ShopId shopId,
            @RequestBody AdminShopItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminShopService.createShopItem(shopId, req));
    }

    @PutMapping("/api/admin/shops/{shopId}/items/{id}")
    public ResponseEntity<ShopItemDto> updateItem(
            @PathVariable ShopId shopId,
            @PathVariable Long id,
            @RequestBody AdminShopItemRequest req) {
        return ResponseEntity.ok(adminShopService.updateShopItem(id, req));
    }

    @DeleteMapping("/api/admin/shops/{shopId}/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable ShopId shopId, @PathVariable Long id) {
        adminShopService.deleteShopItem(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Collections ──────────────────────────────────────────────────────────

    @GetMapping("/api/admin/shops/{shopId}/collections")
    public ResponseEntity<List<CollectionDto>> listCollections(@PathVariable ShopId shopId) {
        return ResponseEntity.ok(adminShopService.listCollections(shopId));
    }

    @PostMapping("/api/admin/shops/{shopId}/collections")
    public ResponseEntity<CollectionDto> createCollection(
            @PathVariable ShopId shopId,
            @RequestBody AdminCollectionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminShopService.createCollection(shopId, req));
    }

    @PutMapping("/api/admin/shops/{shopId}/collections/{id}")
    public ResponseEntity<CollectionDto> updateCollection(
            @PathVariable ShopId shopId,
            @PathVariable Long id,
            @RequestBody AdminCollectionRequest req) {
        return ResponseEntity.ok(adminShopService.updateCollection(id, req));
    }

    @DeleteMapping("/api/admin/shops/{shopId}/collections/{id}")
    public ResponseEntity<Void> deleteCollection(@PathVariable ShopId shopId, @PathVariable Long id) {
        adminShopService.deleteCollection(id);
        return ResponseEntity.noContent().build();
    }
}
