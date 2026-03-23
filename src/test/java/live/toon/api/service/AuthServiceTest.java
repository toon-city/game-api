package live.toon.api.service;

import live.toon.api.dto.AuthRequest;
import live.toon.api.dto.AuthResponse;
import live.toon.api.entity.Gender;
import live.toon.api.entity.User;
import live.toon.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository  userRepository;
    @Mock JwtService      jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthService authService;

    private static final UUID USER_ID = UUID.randomUUID();
    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(USER_ID)
                .username("alice")
                .passwordHash("$2a$hashed")
                .email("alice@example.com")
                .gender(Gender.FEMALE)
                .pez(1500).kreds(0)
                .avatarOptionsJson("{}")
                .build();
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_returnsToken_whenCredentialsValid() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("pass123", "$2a$hashed")).thenReturn(true);
        when(jwtService.generateToken(existingUser)).thenReturn("jwt-token");
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        AuthRequest req = new AuthRequest();
        req.setUsername("alice");
        req.setPassword("pass123");

        AuthResponse resp = authService.login(req);

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getUsername()).isEqualTo("alice");
        assertThat(resp.getUserId()).isEqualTo(USER_ID.toString());
        verify(userRepository).save(existingUser); // lastLoginAt mis à jour
    }

    @Test
    void login_throwsWhenUsernameNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        AuthRequest req = new AuthRequest();
        req.setUsername("unknown");
        req.setPassword("pass");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identifiants incorrects");
    }

    @Test
    void login_throwsWhenPasswordInvalid() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);

        AuthRequest req = new AuthRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identifiants incorrects");
    }

    @Test
    void login_storesHashOnFirstLogin_whenAccountHasNoPassword() {
        existingUser.setPasswordHash(null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newpass")).thenReturn("$2a$newHash");
        when(jwtService.generateToken(existingUser)).thenReturn("token");
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        AuthRequest req = new AuthRequest();
        req.setUsername("alice");
        req.setPassword("newpass");

        authService.login(req);

        assertThat(existingUser.getPasswordHash()).isEqualTo("$2a$newHash");
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_createsUserAndReturnsToken() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("$2a$bobHash");

        User saved = User.builder().id(UUID.randomUUID()).username("bob")
                .passwordHash("$2a$bobHash").email("bob@example.com")
                .pez(1500).kreds(0).avatarOptionsJson("{}").build();
        when(userRepository.save(any())).thenReturn(saved);
        when(jwtService.generateToken(saved)).thenReturn("bob-token");

        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("secret1");
        req.setEmail("bob@example.com");
        req.setGender("MALE");

        AuthResponse resp = authService.register(req);

        assertThat(resp.getToken()).isEqualTo("bob-token");
        assertThat(resp.getUsername()).isEqualTo("bob");
    }

    @Test
    void register_throwsWhenPasswordTooShort() {
        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("ab");
        req.setEmail("bob@example.com");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("au moins 6 caractères");
    }

    @Test
    void register_throwsWhenUsernameTaken() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        AuthRequest req = new AuthRequest();
        req.setUsername("alice");
        req.setPassword("password");
        req.setEmail("other@example.com");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pseudo est déjà utilisé");
    }

    @Test
    void register_throwsWhenEmailTaken() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("password");
        req.setEmail("alice@example.com");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email est déjà utilisée");
    }

    @Test
    void register_throwsWhenEmailMissing() {
        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("password123");
        req.setEmail(null);
        when(userRepository.existsByUsername("bob")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email est obligatoire");
    }

    // ── me ────────────────────────────────────────────────────────────────────

    @Test
    void me_returnsCurrentProfile() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("token");

        AuthResponse resp = authService.me(USER_ID);

        assertThat(resp.getUsername()).isEqualTo("alice");
        assertThat(resp.getPez()).isEqualTo(1500);
    }

    @Test
    void me_throwsWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }
}
