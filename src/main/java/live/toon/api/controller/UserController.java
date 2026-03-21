package live.toon.api.controller;

import live.toon.api.dto.UserDto;
import live.toon.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Liste les utilisateurs de manière paginée avec recherche par pseudo.
     * Accessible publiquement (nécessaire pour le panneau joueurs sans auth).
     */
    @GetMapping
    public Page<UserDto> listUsers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        int clampedSize = Math.min(size, 50);
        return userRepository
                .findByUsernameContainingIgnoreCaseOrderByUsernameAsc(q, PageRequest.of(page, clampedSize))
                .map(u -> UserDto.builder()
                        .username(u.getUsername())
                        .gender(u.getGender() != null ? u.getGender().name() : null)
                        .rank(u.getRank())
                        .toonizLevel(u.getToonizLevel())
                        .lastLoginAt(u.getLastLoginAt())
                        .build());
    }
}
