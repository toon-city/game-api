package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.MetierOptionDto;
import live.toon.api.entity.Metier;
import live.toon.api.entity.User;
import live.toon.api.repository.MetierRepository;
import live.toon.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Player-facing side of the métier system: list the catalogue with
 * eligibility computed against the current user, and let them pick one.
 * Separate from AdminMetierService (pure catalogue CRUD, no user context)
 * the same way FriendService/MairieService split "manage the thing" from
 * "act on it as a player".
 */
@Service
@RequiredArgsConstructor
public class MetierAssignmentService {

    private static final int COOLDOWN_DAYS = 7;

    private final MetierRepository metierRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MetierOptionDto> listOptions(UUID userId) {
        User user = requireUser(userId);
        return metierRepository.findAll(Sort.by("name").ascending()).stream()
                .map(m -> toOptionDto(m, user))
                .toList();
    }

    @Transactional
    public MetierOptionDto choose(UUID userId, Long metierId) {
        User user = requireUser(userId);
        Metier metier = metierRepository.findById(metierId)
                .orElseThrow(() -> new EntityNotFoundException("Métier introuvable : " + metierId));

        String blockReason = blockReason(metier, user);
        if (blockReason != null) {
            // IllegalArgumentException, not IllegalStateException — GlobalExceptionHandler
            // maps this to 400 + a `.message` the frontend surfaces as-is (same
            // convention AuthService/FriendService use for user-facing rejections).
            throw new IllegalArgumentException(blockReason);
        }

        user.setMetier(metier);
        user.setMetierChangedAt(OffsetDateTime.now());
        userRepository.save(user);

        return toOptionDto(metier, user);
    }

    private MetierOptionDto toOptionDto(Metier m, User user) {
        String blockReason = blockReason(m, user);
        return MetierOptionDto.builder()
                .id(m.getId())
                .name(m.getName())
                .dailyPez(m.getDailyPez())
                .minToonizLevel(m.getMinToonizLevel())
                .minDaysPlayed(m.getMinDaysPlayed())
                .eligible(blockReason == null)
                .blockReason(blockReason)
                .build();
    }

    /** Null = éligible. Checked in a fixed, most-restrictive-first order so the UI always shows ONE clear reason. */
    private String blockReason(Metier metier, User user) {
        OffsetDateTime changedAt = user.getMetierChangedAt();
        if (changedAt != null) {
            OffsetDateTime unlocksAt = changedAt.plusDays(COOLDOWN_DAYS);
            if (unlocksAt.isAfter(OffsetDateTime.now())) {
                long daysLeft = ChronoUnit.DAYS.between(OffsetDateTime.now(), unlocksAt) + 1;
                return "Vous avez changé de métier récemment — réessayez dans " + daysLeft
                        + (daysLeft > 1 ? " jours." : " jour.");
            }
        }
        if (metier.getMinToonizLevel() != null && user.getToonizLevel() < metier.getMinToonizLevel()) {
            return "Niveau Tooniz insuffisant (" + toonizLabel(metier.getMinToonizLevel()) + " requis).";
        }
        if (metier.getMinDaysPlayed() != null && user.getDaysPlayed() < metier.getMinDaysPlayed()) {
            return "Il faut avoir joué au moins " + metier.getMinDaysPlayed() + " jours.";
        }
        return null;
    }

    private String toonizLabel(int level) {
        return switch (level) {
            case 1 -> "Bronze";
            case 2 -> "Argent";
            case 3 -> "Or";
            default -> "niveau " + level;
        };
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
    }
}
