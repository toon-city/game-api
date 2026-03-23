package live.toon.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.toon.api.config.SecurityConfig;
import live.toon.api.dto.AuthRequest;
import live.toon.api.dto.AuthResponse;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.ResourcePermissionEvaluator;
import live.toon.api.security.UserRank;
import live.toon.api.service.AuthService;
import live.toon.api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService                authService;
    @MockBean JwtService                 jwtService;
    @MockBean UserRepository             userRepository;
    @MockBean ResourcePermissionEvaluator resourcePermissionEvaluator;

    // ── POST /api/auth/token ──────────────────────────────────────────────────

    @Test
    void login_returns200_withValidCredentials() throws Exception {
        AuthResponse resp = buildResponse("alice");
        when(authService.login(any())).thenReturn(resp);

        AuthRequest req = new AuthRequest();
        req.setUsername("alice");
        req.setPassword("pass123");

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.token", is("jwt-token")));
    }

    @Test
    void login_returns400_whenUsernameBlank() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setUsername("");
        req.setPassword("pass");

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns400_whenUsernameHasInvalidChars() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setUsername("alice@bad!");
        req.setPassword("pass");

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @Test
    void register_returns200_withValidData() throws Exception {
        AuthResponse resp = buildResponse("bob");
        when(authService.register(any())).thenReturn(resp);

        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("secure1");
        req.setEmail("bob@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("bob")));
    }

    @Test
    void register_returns400_whenEmailInvalid() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("secure1");
        req.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────────

    @Test
    void me_returns200_whenAuthenticated() throws Exception {
        AuthResponse resp = buildResponse("alice");
        when(authService.me(any())).thenReturn(resp);

        mockMvc.perform(get("/api/auth/me").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("alice")));
    }

    @Test
    void me_returns401_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RequestPostProcessor auth() {
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), "alice", UserRank.ROLE_USER);
        Authentication token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(token);
    }

    private AuthResponse buildResponse(String username) {
        return AuthResponse.builder()
                .token("jwt-token")
                .userId(UUID.randomUUID().toString())
                .username(username)
                .rank(0).toonizLevel(0).kreds(0).pez(1500)
                .avatarOptionsJson("{}")
                .build();
    }
}
