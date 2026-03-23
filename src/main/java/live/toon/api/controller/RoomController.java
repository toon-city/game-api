package live.toon.api.controller;

import live.toon.api.dto.ChatMessageDto;
import live.toon.api.dto.RoomDto;
import live.toon.api.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /** Liste toutes les rooms publiques (rétrocompatibilité). */
    @GetMapping
    public ResponseEntity<List<RoomDto>> listRooms() {
        return ResponseEntity.ok(roomService.getPublicRooms());
    }

    /**
     * Liste paginée des rooms publiques.
     * Sans recherche : uniquement les rooms avec au moins 1 joueur (trié userCount DESC → name ASC).
     * Avec recherche : toutes les rooms correspondantes.
     */
    @GetMapping("/paged")
    public ResponseEntity<Page<RoomDto>> listRoomsPaged(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(roomService.getPublicRoomsPaged(q, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDto> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoom(id));
    }

    @GetMapping("/{id}/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(roomService.getChatHistory(id, Math.min(limit, 200)));
    }
}
