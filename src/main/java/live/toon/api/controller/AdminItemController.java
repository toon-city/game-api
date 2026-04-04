package live.toon.api.controller;

import live.toon.api.dto.AdminItemRequest;
import live.toon.api.dto.ItemDto;
import live.toon.api.service.AdminItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminItemController {

    private final AdminItemService adminItemService;

    @GetMapping
    public ResponseEntity<Page<ItemDto>> listItems(@RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(adminItemService.listItems(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(adminItemService.getItem(id));
    }

    @PostMapping
    public ResponseEntity<ItemDto> createItem(@RequestBody AdminItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminItemService.createItem(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(@PathVariable Long id, @RequestBody AdminItemRequest req) {
        return ResponseEntity.ok(adminItemService.updateItem(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        adminItemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
