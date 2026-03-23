package live.toon.api.controller;

import live.toon.api.dto.ItemDto;
import live.toon.api.dto.UserItemDto;
import live.toon.api.entity.ItemSubType;
import live.toon.api.entity.ItemType;
import live.toon.api.config.SecurityConfig;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.ResourcePermissionEvaluator;
import live.toon.api.security.UserRank;
import live.toon.api.service.InventoryService;
import live.toon.api.service.JwtService;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import(SecurityConfig.class)
class InventoryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean InventoryService           inventoryService;
    @MockBean JwtService                 jwtService;
    @MockBean UserRepository             userRepository;
    @MockBean ResourcePermissionEvaluator resourcePermissionEvaluator;

    // ── GET /api/inventory ────────────────────────────────────────────────────

    @Test
    void listInventory_returns200_whenAuthenticated() throws Exception {
        UserItemDto dto = buildUserItemDto(10L, false);
        when(inventoryService.listItems(any(), isNull(), eq(0)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/inventory").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(10)));
    }

    @Test
    void listInventory_returns401_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listInventory_withTypeFilter_passesTypeToService() throws Exception {
        when(inventoryService.listItems(any(), eq(ItemType.CLOTHING), eq(0)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/inventory")
                        .param("type", "CLOTHING")
                        .with(auth()))
                .andExpect(status().isOk());

        verify(inventoryService).listItems(any(), eq(ItemType.CLOTHING), eq(0));
    }

    // ── PUT /api/inventory/{id}/equip ─────────────────────────────────────────

    @Test
    void equip_returns200_whenAuthenticated() throws Exception {
        UserItemDto dto = buildUserItemDto(10L, true);
        when(inventoryService.equipItem(any(), eq(10L))).thenReturn(dto);

        mockMvc.perform(put("/api/inventory/10/equip").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.equipped", is(true)));
    }

    @Test
    void equip_returns401_withoutAuth() throws Exception {
        mockMvc.perform(put("/api/inventory/10/equip"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/inventory/{id}/unequip ───────────────────────────────────────

    @Test
    void unequip_returns200_whenAuthenticated() throws Exception {
        UserItemDto dto = buildUserItemDto(10L, false);
        when(inventoryService.unequipItem(any(), eq(10L))).thenReturn(dto);

        mockMvc.perform(put("/api/inventory/10/unequip").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.equipped", is(false)));
    }

    @Test
    void unequip_returns401_withoutAuth() throws Exception {
        mockMvc.perform(put("/api/inventory/10/unequip"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RequestPostProcessor auth() {
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), "alice", UserRank.ROLE_USER);
        Authentication token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(token);
    }

    private UserItemDto buildUserItemDto(Long id, boolean equipped) {
        ItemDto item = ItemDto.builder()
                .id(1L).name("Tshirt April").itemType("CLOTHING").subType("TOP")
                .possessable(true).displayImage("img/shirt.png")
                .spritePath("assets/shirt.json").spriteKey("top")
                .build();
        return UserItemDto.builder()
                .id(id).item(item).equipped(equipped)
                .build();
    }
}
