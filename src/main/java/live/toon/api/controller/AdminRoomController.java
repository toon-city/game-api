package live.toon.api.controller;

import live.toon.api.dto.AdminRoomDto;
import live.toon.api.dto.AdminRoomUpdateRequest;
import live.toon.api.service.AdminRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    @GetMapping
    public ResponseEntity<Page<AdminRoomDto>> listRooms(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(adminRoomService.listRooms(search, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminRoomDto> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(adminRoomService.getRoom(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminRoomDto> updateRoom(
            @PathVariable Long id,
            @RequestBody AdminRoomUpdateRequest req) {
        return ResponseEntity.ok(adminRoomService.updateRoom(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        adminRoomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<AdminRoomDto> lock(@PathVariable Long id) {
        return ResponseEntity.ok(adminRoomService.lockRoom(id));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<AdminRoomDto> unlock(@PathVariable Long id) {
        return ResponseEntity.ok(adminRoomService.unlockRoom(id));
    }

    @PostMapping("/{id}/kick")
    public ResponseEntity<AdminRoomDto> kick(@PathVariable Long id) {
        return ResponseEntity.ok(adminRoomService.kickAll(id));
    }
}
