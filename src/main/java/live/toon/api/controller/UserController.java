package live.toon.api.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import live.toon.api.dto.UserDto;
import live.toon.api.dto.UserUpdateRequest;
import live.toon.api.entity.Gender;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    public UserPageResponse listUsers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        int clampedSize = Math.min(size, 50);
        Sort sort = Sort.by(
                Sort.Order.desc("online"),
                Sort.Order.desc("lastLoginAt"),
                Sort.Order.asc("username")
        );
        Page<UserDto> pageResult = userRepository
                .findByUsernameContainingIgnoreCase(q, PageRequest.of(page, clampedSize, sort))
                .map(u -> UserDto.builder()
                        .username(u.getUsername())
                        .gender(u.getGender() != null ? u.getGender().name() : null)
                        .rank(u.getRank())
                        .toonizLevel(u.getToonizLevel())
                        .lastLoginAt(u.getLastLoginAt())
                        .online(u.isOnline())
                        .currentRoomId(u.getCurrentRoomId())
                        .build());

        return new UserPageResponse(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),
                pageResult.getSize(),
                userRepository.countByOnlineTrue()
        );
    }

    /**
     * Met à jour le profil d'un utilisateur.
     * Autorisé uniquement par l'utilisateur lui-même ou un admin.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'User', 'update')")
    @Transactional
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @RequestBody UserUpdateRequest request) {

        var user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }
        if (request.getGender() != null && !request.getGender().isBlank()) {
            try {
                user.setGender(Gender.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException ignored) { /* genre inconnu ignoré */ }
        }

        var saved = userRepository.save(user);
        return ResponseEntity.ok(UserDto.builder()
                .username(saved.getUsername())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .rank(saved.getRank())
                .toonizLevel(saved.getToonizLevel())
                .lastLoginAt(saved.getLastLoginAt())
                .online(saved.isOnline())
                .currentRoomId(saved.getCurrentRoomId())
                .build());
    }

    public record UserPageResponse(
            List<UserDto> content,
            long totalElements,
            int totalPages,
            int number,
            int size,
            long onlineCount
    ) {}

    /** PUT /api/users/me/skin-color — persiste la couleur de peau de l'utilisateur connecté. */
    @PutMapping("/me/skin-color")
    @Transactional
    public ResponseEntity<Void> updateSkinColor(
            @RequestBody SkinColorRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        var user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
        user.setSkinColor(request.skinColor());
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    public record SkinColorRequest(Integer skinColor) {}
}
