package live.toon.api.service;

import live.toon.api.dto.AdminUserDto;
import live.toon.api.dto.BanRequest;
import live.toon.api.entity.User;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.UserRank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int PAGE_SIZE = 30;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserDto> listUsers(String search, Boolean banned, int page) {
        PageRequest pr = PageRequest.of(page, PAGE_SIZE, Sort.by("username").ascending());
        boolean hasSearch = search != null && !search.isBlank();
        Page<User> users;
        if (hasSearch && banned != null) {
            users = userRepository.findByUsernameContainingIgnoreCaseAndBanned(search, banned, pr);
        } else if (hasSearch) {
            users = userRepository.findByUsernameContainingIgnoreCase(search, pr);
        } else if (banned != null) {
            users = userRepository.findByBanned(banned, pr);
        } else {
            users = userRepository.findAll(pr);
        }
        return users.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDto getUser(UUID id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    @Transactional
    public AdminUserDto banUser(JwtPrincipal actor, UUID targetId, BanRequest req) {
        User banner = userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Acteur introuvable"));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        // Un modérateur ne peut pas bannir un admin ou un autre modérateur
        boolean actorIsAdmin = actor.getRank().isAtLeast(UserRank.ROLE_ADMIN);
        if (!actorIsAdmin && target.getRank() >= UserRank.ROLE_MODERATOR.getLevel()) {
            throw new IllegalArgumentException("Permissions insuffisantes pour bannir cet utilisateur");
        }
        // Personne ne peut se bannir lui-même
        if (actor.getUserId().equals(targetId)) {
            throw new IllegalArgumentException("Impossible de se bannir soi-même");
        }

        target.setBanned(true);
        target.setBanReason(req.reason());
        target.setBannedAt(OffsetDateTime.now());
        target.setBannedBy(banner);
        return toDto(userRepository.save(target));
    }

    @Transactional
    public AdminUserDto unbanUser(UUID targetId) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        target.setBanned(false);
        target.setBanReason(null);
        target.setBannedAt(null);
        target.setBannedBy(null);
        return toDto(userRepository.save(target));
    }

    @Transactional
    public AdminUserDto updateRank(UUID targetId, int rank) {
        if (rank < 0 || rank > 2) throw new IllegalArgumentException("Rang invalide (0-2)");
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        target.setRank(rank);
        return toDto(userRepository.save(target));
    }

    public AdminUserDto toDto(User u) {
        return AdminUserDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .gender(u.getGender() != null ? u.getGender().name() : null)
                .rank(u.getRank())
                .toonizLevel(u.getToonizLevel())
                .kreds(u.getKreds())
                .pez(u.getPez())
                .online(u.isOnline())
                .banned(u.isBanned())
                .banReason(u.getBanReason())
                .bannedAt(u.getBannedAt())
                .bannedById(u.getBannedBy() != null ? u.getBannedBy().getId() : null)
                .bannedByUsername(u.getBannedBy() != null ? u.getBannedBy().getUsername() : null)
                .createdAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }
}
