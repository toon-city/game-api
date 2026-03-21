package live.toon.api.controller;

import jakarta.validation.Valid;
import live.toon.api.dto.DeditoonDto;
import live.toon.api.dto.DeditoonRequest;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.DeditoonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deditoons")
@RequiredArgsConstructor
public class DeditoonController {

    private final DeditoonService deditoonService;

    /** Retourne les 10 dernières déditoons (public). */
    @GetMapping
    public ResponseEntity<List<DeditoonDto>> getLatest() {
        return ResponseEntity.ok(deditoonService.getLatest());
    }

    /** Publie une déditoon (authentifié, coûte 10 kreds). */
    @PostMapping
    public ResponseEntity<DeditoonDto> post(
            @Valid @RequestBody DeditoonRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        DeditoonDto created = deditoonService.post(principal.getUserId(), request.getMessage());
        return ResponseEntity.ok(created);
    }
}
