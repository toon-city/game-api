package live.toon.api.controller;

import live.toon.api.dto.FriendTargetRequest;
import live.toon.api.dto.FriendsStatusDto;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/status")
    public ResponseEntity<FriendsStatusDto> status(@AuthenticationPrincipal JwtPrincipal actor) {
        return ResponseEntity.ok(friendService.status(actor));
    }

    @PostMapping("/requests")
    public ResponseEntity<Void> sendRequest(
            @AuthenticationPrincipal JwtPrincipal actor,
            @RequestBody FriendTargetRequest request) {
        friendService.sendRequest(actor, request.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<Void> accept(@AuthenticationPrincipal JwtPrincipal actor, @PathVariable Long id) {
        friendService.respond(actor, id, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{id}/decline")
    public ResponseEntity<Void> decline(@AuthenticationPrincipal JwtPrincipal actor, @PathVariable Long id) {
        friendService.respond(actor, id, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{id}/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal JwtPrincipal actor, @PathVariable Long id) {
        friendService.cancel(actor, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeFriend(@AuthenticationPrincipal JwtPrincipal actor, @PathVariable UUID userId) {
        friendService.removeFriend(actor, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/blocks")
    public ResponseEntity<Void> block(
            @AuthenticationPrincipal JwtPrincipal actor,
            @RequestBody FriendTargetRequest request) {
        friendService.block(actor, request.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/blocks/{userId}")
    public ResponseEntity<Void> unblock(@AuthenticationPrincipal JwtPrincipal actor, @PathVariable UUID userId) {
        friendService.unblock(actor, userId);
        return ResponseEntity.noContent().build();
    }
}
