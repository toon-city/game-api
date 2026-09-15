package live.toon.api.controller;

import live.toon.api.dto.MetierOptionDto;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.MetierAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metiers")
@RequiredArgsConstructor
public class MetierController {

    private final MetierAssignmentService metierAssignmentService;

    @GetMapping
    public ResponseEntity<List<MetierOptionDto>> listOptions(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(metierAssignmentService.listOptions(principal.getUserId()));
    }

    @PostMapping("/choose")
    public ResponseEntity<MetierOptionDto> choose(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(metierAssignmentService.choose(principal.getUserId(), body.get("metierId")));
    }
}
