package live.toon.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.toon.api.config.SecurityConfig;
import live.toon.api.dto.EnterHouseRequest;
import live.toon.api.dto.HouseDto;
import live.toon.api.dto.HouseRequest;
import live.toon.api.dto.HouseSchemaDto;
import live.toon.api.entity.HouseAccess;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.ResourcePermissionEvaluator;
import live.toon.api.security.UserRank;
import live.toon.api.service.HouseService;
import live.toon.api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HouseController.class)
@Import(SecurityConfig.class)
class HouseControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean HouseService               houseService;
    @MockBean JwtService                 jwtService;
    @MockBean UserRepository             userRepository;
    @MockBean ResourcePermissionEvaluator resourcePermissionEvaluator;

    // ── GET /api/house-schemas — public ───────────────────────────────────────

    @Test
    void listSchemas_returns200_withoutAuth() throws Exception {
        when(houseService.listSchemas()).thenReturn(List.of(
                HouseSchemaDto.builder().id(1L).name("Chalet").description("Un chalet").build()));

        mockMvc.perform(get("/api/house-schemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Chalet")));
    }

    // ── GET /api/houses — public ──────────────────────────────────────────────

    @Test
    void listHouses_returns200_withoutAuth() throws Exception {
        when(houseService.listPrivateHouses()).thenReturn(List.of(buildHouseDto(10L, "Chez Alice")));

        mockMvc.perform(get("/api/houses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Chez Alice")));
    }

    // ── GET /api/houses/mine — auth requise ───────────────────────────────────

    @Test
    void myHouses_returns200_whenAuthenticated() throws Exception {
        when(houseService.listMyHouses(any())).thenReturn(List.of(buildHouseDto(10L, "Chez Alice")));

        mockMvc.perform(get("/api/houses/mine").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(10)));
    }

    @Test
    void myHouses_returns401_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/houses/mine"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/houses — auth requise ───────────────────────────────────────

    @Test
    void createHouse_returns201_whenAuthenticated() throws Exception {
        HouseDto created = buildHouseDto(10L, "Mon Chalet");
        when(houseService.createHouse(any(), any())).thenReturn(created);

        HouseRequest req = new HouseRequest();
        req.setName("Mon Chalet");
        req.setSchemaId(1L);
        req.setAccess(HouseAccess.OPEN);

        mockMvc.perform(post("/api/houses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(auth()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/houses/10"))
                .andExpect(jsonPath("$.name", is("Mon Chalet")));
    }

    @Test
    void createHouse_returns401_withoutAuth() throws Exception {
        HouseRequest req = new HouseRequest();
        req.setName("Test");
        req.setSchemaId(1L);
        req.setAccess(HouseAccess.OPEN);

        mockMvc.perform(post("/api/houses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createHouse_returns400_whenNameBlank() throws Exception {
        HouseRequest req = new HouseRequest();
        req.setName("");
        req.setSchemaId(1L);
        req.setAccess(HouseAccess.OPEN);

        mockMvc.perform(post("/api/houses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(auth()))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/houses/{id} — auth requise ────────────────────────────────

    @Test
    void deleteHouse_returns204_whenAuthorized() throws Exception {
        when(resourcePermissionEvaluator.hasPermission(any(), eq(10L), eq("House"), eq("delete"))).thenReturn(true);
        doNothing().when(houseService).deleteHouse(10L);

        mockMvc.perform(delete("/api/houses/10").with(auth()))
                .andExpect(status().isNoContent());
    }

    // ── POST /api/houses/{id}/enter ───────────────────────────────────────────

    @Test
    void enterHouse_returns200_withCorrectPassword() throws Exception {
        doNothing().when(houseService).validateAccess(any(), eq(10L), eq("secret"));

        EnterHouseRequest req = new EnterHouseRequest();
        req.setPassword("secret");

        mockMvc.perform(post("/api/houses/10/enter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(auth()))
                .andExpect(status().isOk());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RequestPostProcessor auth() {
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), "alice", UserRank.ROLE_USER);
        Authentication token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(token);
    }

    private RequestPostProcessor adminAuth() {
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), "admin", UserRank.ROLE_ADMIN);
        Authentication token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(token);
    }

    private HouseDto buildHouseDto(Long id, String name) {
        return HouseDto.builder()
                .id(id).name(name).type("PRIVATE").access("OPEN")
                .ownerUsername("alice").hasPassword(false)
                .schemaId(1L).schemaName("Chalet")
                .maxUsers(20).userCount(0)
                .build();
    }
}
