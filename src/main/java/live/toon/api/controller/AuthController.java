package live.toon.api.controller;

import jakarta.validation.Valid;
import live.toon.api.dto.AuthRequest;
import live.toon.api.dto.AuthResponse;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/token — login */
    @PostMapping("/token")
    public ResponseEntity<AuthResponse> token(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** POST /api/auth/register — inscription */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /** GET /api/auth/me — profil courant rafraîchi (kreds, pez, rang…) */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal.getUserId()));
    }
}
