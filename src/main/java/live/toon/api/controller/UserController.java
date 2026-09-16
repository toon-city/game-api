package live.toon.api.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import live.toon.api.dto.EquippedItemDto;
import live.toon.api.dto.UserDto;
import live.toon.api.dto.UserProfileDto;
import live.toon.api.dto.UserUpdateRequest;
import live.toon.api.entity.Gender;
import live.toon.api.entity.User;
import live.toon.api.repository.UserItemRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.WorkOutfitService;
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
    private final UserItemRepository userItemRepository;
    private final WorkOutfitService workOutfitService;

    /**
     * Liste les utilisateurs de manière paginée avec recherche par pseudo.
     * Accessible publiquement (nécessaire pour le panneau joueurs sans auth).
     */
    @GetMapping
    @Transactional
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
                        .id(u.getId())
                        .username(u.getUsername())
                        .gender(u.getGender() != null ? u.getGender().name() : null)
                        .rank(u.getRank())
                        .toonizLevel(u.getToonizLevel())
                        .lastLoginAt(u.getLastLoginAt())
                        .online(u.isOnline())
                        .currentRoomId(u.getCurrentRoomId())
                        .married(u.getMarriedTo() != null)
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
        // description/job: applied even blank (unlike email/gender above) —
        // clearing a bio or title is a legitimate edit, not a no-op.
        if (request.getDescription() != null) {
            user.setDescription(request.getDescription());
        }
        if (request.getJob() != null) {
            user.setJob(request.getJob());
        }
        if (request.getWorkOutfitActive() != null) {
            user.setWorkOutfitActive(request.getWorkOutfitActive());
        }

        var saved = userRepository.save(user);
        return ResponseEntity.ok(UserDto.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .rank(saved.getRank())
                .toonizLevel(saved.getToonizLevel())
                .lastLoginAt(saved.getLastLoginAt())
                .online(saved.isOnline())
                .currentRoomId(saved.getCurrentRoomId())
                .married(saved.getMarriedTo() != null)
                .build());
    }

    /**
     * Page profil complète : avatar + slots équipés (bague comprise, même
     * sans sprite) + infos de base + description/job. Lecture publique
     * (comme listUsers) — n'importe qui peut consulter le profil d'un autre
     * joueur ; seule l'édition (PUT /{id}) est restreinte au titulaire/admin.
     */
    @GetMapping("/{id}/profile")
    @Transactional
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        var equippedItems = userItemRepository.findAllEquipped(user);

        List<EquippedItemDto> equipped = equippedItems.stream()
                .map(ui -> EquippedItemDto.builder()
                        .userItemId(ui.getId())
                        .subType(ui.getItem().getSubType().name())
                        .name(ui.getItem().getName())
                        .displayImage(ui.getItem().getDisplayImage())
                        .build())
                .toList();

        // spriteKey -> spritePath — same convention as MarriageService.toSpouseDto(),
        // only the items that actually have sprite art (a ring never does).
        var clothing = equippedItems.stream()
                .map(live.toon.api.entity.UserItem::getItem)
                .filter(item -> item.getSpriteKey() != null && item.getSpritePath() != null)
                .collect(java.util.stream.Collectors.toMap(
                        live.toon.api.entity.Item::getSpriteKey,
                        live.toon.api.entity.Item::getSpritePath,
                        (a, b) -> a));
        clothing = workOutfitService.applyOverlay(user, clothing);

        User spouse = user.getMarriedTo();
        return ResponseEntity.ok(UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .rank(user.getRank())
                .toonizLevel(user.getToonizLevel())
                .createdAt(user.getCreatedAt())
                .job(user.getJob())
                .description(user.getDescription())
                .skinColor(user.getSkinColor())
                .clothing(clothing)
                .marriedToUsername(spouse != null ? spouse.getUsername() : null)
                .marriedAt(user.getMarriedAt())
                .equippedItems(equipped)
                .workOutfitActive(user.isWorkOutfitActive())
                .metierName(user.getMetier() != null ? user.getMetier().getName() : null)
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
