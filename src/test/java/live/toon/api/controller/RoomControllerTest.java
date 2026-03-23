package live.toon.api.controller;

import live.toon.api.config.SecurityConfig;
import live.toon.api.dto.ChatMessageDto;
import live.toon.api.dto.RoomDto;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.ResourcePermissionEvaluator;
import live.toon.api.security.UserRank;
import live.toon.api.service.JwtService;
import live.toon.api.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@Import(SecurityConfig.class)
class RoomControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean RoomService                roomService;
    @MockBean JwtService                 jwtService;
    @MockBean UserRepository             userRepository;
    @MockBean ResourcePermissionEvaluator resourcePermissionEvaluator;

    // ── GET /api/rooms — public ───────────────────────────────────────────────

    @Test
    void listRooms_returns200_withoutAuth() throws Exception {
        when(roomService.getPublicRooms()).thenReturn(List.of(buildRoomDto(1L, "Jardin")));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Jardin")));
    }

    @Test
    void listRoomsPaged_returns200_withoutAuth() throws Exception {
        when(roomService.getPublicRoomsPaged(eq(""), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(buildRoomDto(1L, "Café"))));

        mockMvc.perform(get("/api/rooms/paged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void listRoomsPaged_passesQueryParam() throws Exception {
        when(roomService.getPublicRoomsPaged(eq("jardin"), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(buildRoomDto(1L, "Jardin"))));

        mockMvc.perform(get("/api/rooms/paged").param("q", "jardin"))
                .andExpect(status().isOk());
    }

    // ── GET /api/rooms/{id} — public ──────────────────────────────────────────

    @Test
    void getRoom_returns200_withoutAuth() throws Exception {
        when(roomService.getRoom(1L)).thenReturn(buildRoomDto(1L, "Jardin"));

        mockMvc.perform(get("/api/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.type", is("PUBLIC")));
    }

    // ── GET /api/rooms/{id}/chat — protégé ────────────────────────────────────

    @Test
    void getChatHistory_returns200_whenAuthenticated() throws Exception {
        ChatMessageDto msg = ChatMessageDto.builder()
                .id(1L).roomId(1L).userId(UUID.randomUUID().toString())
                .username("alice").message("Salut !").sentAt("2026-01-01T10:00:00Z")
                .build();
        when(roomService.getChatHistory(eq(1L), anyInt())).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/rooms/1/chat").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("alice")));
    }

    @Test
    void getChatHistory_returns401_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/rooms/1/chat"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RequestPostProcessor auth() {
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), "alice", UserRank.ROLE_USER);
        Authentication token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(token);
    }

    private RoomDto buildRoomDto(Long id, String name) {
        return RoomDto.builder()
                .id(id).name(name).houseData("<map/>")
                .maxUsers(50).userCount(2).type("PUBLIC")
                .build();
    }
}
