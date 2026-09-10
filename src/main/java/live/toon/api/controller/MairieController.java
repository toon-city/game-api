package live.toon.api.controller;

import live.toon.api.dto.ConvertPezRequest;
import live.toon.api.dto.JustMarriedDto;
import live.toon.api.dto.MairieStatusDto;
import live.toon.api.dto.ProposeMarriageRequest;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.MarriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mairie")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MairieController {

    private final MarriageService marriageService;

    @GetMapping("/status")
    public ResponseEntity<MairieStatusDto> status(@AuthenticationPrincipal JwtPrincipal actor) {
        return ResponseEntity.ok(marriageService.status(actor));
    }

    /** Panneau "just married" de l'accueil — le dernier mariage accepté, site entier. */
    @GetMapping("/last-married")
    public ResponseEntity<JustMarriedDto> lastMarried() {
        return marriageService.lastMarried()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/propose")
    public ResponseEntity<Void> propose(
            @AuthenticationPrincipal JwtPrincipal actor,
            @RequestBody ProposeMarriageRequest request) {
        marriageService.propose(actor, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/proposals/{id}/accept")
    public ResponseEntity<Void> accept(
            @AuthenticationPrincipal JwtPrincipal actor,
            @PathVariable Long id) {
        marriageService.respond(actor, id, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/proposals/{id}/decline")
    public ResponseEntity<Void> decline(
            @AuthenticationPrincipal JwtPrincipal actor,
            @PathVariable Long id) {
        marriageService.respond(actor, id, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/proposals/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal JwtPrincipal actor,
            @PathVariable Long id) {
        marriageService.cancel(actor, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/convert")
    public ResponseEntity<Void> convert(
            @AuthenticationPrincipal JwtPrincipal actor,
            @RequestBody ConvertPezRequest request) {
        marriageService.convertPezToKred(actor, request);
        return ResponseEntity.noContent().build();
    }
}
