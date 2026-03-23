package live.toon.api.service;

import live.toon.api.dto.AuthRequest;
import live.toon.api.dto.AuthResponse;
import live.toon.api.entity.Gender;
import live.toon.api.entity.User;
import live.toon.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Login — vérifie les identifiants et retourne un JWT.
     */
    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Identifiants incorrects"));

        // Si un hash est stocké, on valide le mot de passe
        if (user.getPasswordHash() != null) {
            if (request.getPassword() == null ||
                    !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Identifiants incorrects");
            }
        } else if (request.getPassword() != null) {
            // Compte sans mot de passe → on enregistre le hash fourni (migration)
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        return buildResponse(user);
    }

    /**
     * Inscription — crée un compte avec nom d'utilisateur + mot de passe.
     */
    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Ce pseudo est déjà utilisé");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("L'adresse email est obligatoire");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Cette adresse email est déjà utilisée");
        }
        Gender gender = null;
        if (request.getGender() != null && !request.getGender().isBlank()) {
            try {
                gender = Gender.valueOf(request.getGender().toUpperCase());
            } catch (IllegalArgumentException ignored) { /* genre inconnu ignoré */ }
        }
        User user = userRepository.save(User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .gender(gender)
                .email(request.getEmail())
                .build());
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .userId(user.getId().toString())
                .username(user.getUsername())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .rank(user.getRank())
                .toonizLevel(user.getToonizLevel())
                .kreds(user.getKreds())
                .pez(user.getPez())
                .avatarOptionsJson(user.getAvatarOptionsJson())
                .build();
    }

    /** Retourne un profil à jour (sans nouveau token) pour rafraîchir le frontend. */
    @Transactional(readOnly = true)
    public AuthResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        return buildResponse(user);
    }
}

