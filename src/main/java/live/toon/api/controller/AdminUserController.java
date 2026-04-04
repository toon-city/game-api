package live.toon.api.controller;

import live.toon.api.dto.AdminUserDto;
import live.toon.api.dto.BanRequest;
import live.toon.api.dto.RankUpdateRequest;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_MODERATOR','ROLE_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Page<AdminUserDto>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean banned,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(adminUserService.listUsers(search, banned, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getUser(id));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<AdminUserDto> ban(
            @AuthenticationPrincipal JwtPrincipal actor,
            @PathVariable UUID id,
            @RequestBody BanRequest req) {
        return ResponseEntity.ok(adminUserService.banUser(actor, id, req));
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<AdminUserDto> unban(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.unbanUser(id));
    }

    @PutMapping("/{id}/rank")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdminUserDto> updateRank(
            @PathVariable UUID id,
            @RequestBody RankUpdateRequest req) {
        return ResponseEntity.ok(adminUserService.updateRank(id, req.rank()));
    }
}
