package live.toon.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.toon.api.dto.BuyOptionRequest;
import live.toon.api.dto.BuyOptionRequest.BuyOption;
import live.toon.api.dto.CollectionDto;
import live.toon.api.dto.ItemDto;
import live.toon.api.dto.ShopItemDto;
import live.toon.api.dto.UserItemDto;
import live.toon.api.entity.ItemType;
import live.toon.api.entity.ShopId;
import live.toon.api.config.SecurityConfig;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.ResourcePermissionEvaluator;
import live.toon.api.security.UserRank;
import live.toon.api.service.JwtService;
import live.toon.api.service.ShopService;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShopController.class)
@Import(SecurityConfig.class)
class ShopControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean ShopService                shopService;
    @MockBean JwtService                 jwtService;
    @MockBean UserRepository             userRepository;
    @MockBean ResourcePermissionEvaluator resourcePermissionEvaluator;

    // ── GET /api/shops/{shopId}/items — route PUBLIQUE ────────────────────────

    @Test
    void listItems_returns200_withoutAuthentication() throws Exception {
        when(shopService.listItems(eq(ShopId.COUPE_TIFF), isNull(), eq(0)))
                .thenReturn(new PageImpl<>(List.of(buildShopItemDto(1L))));

        mockMvc.perform(get("/api/shops/COUPE_TIFF/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void listItems_withCollectionFilter_callsService() throws Exception {
        when(shopService.listItems(eq(ShopId.COUPE_TIFF), eq(3L), eq(0)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/shops/COUPE_TIFF/items")
                        .param("collectionId", "3"))
                .andExpect(status().isOk());

        verify(shopService).listItems(ShopId.COUPE_TIFF, 3L, 0);
    }

    @Test
    void listItems_withPageParam_callsService() throws Exception {
        when(shopService.listItems(eq(ShopId.IKEBO), isNull(), eq(2)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/shops/IKEBO/items").param("page", "2"))
                .andExpect(status().isOk());

        verify(shopService).listItems(ShopId.IKEBO, null, 2);
    }

    // ── GET /api/shops/{shopId}/collections — route PUBLIQUE ─────────────────

    @Test
    void listCollections_returns200_withoutAuthentication() throws Exception {
        when(shopService.listCollections(ShopId.VESTIS)).thenReturn(List.of(
                CollectionDto.builder().id(1L).shopId("VESTIS").name("Collection Pâques")
                        .bannerImage("/img/paques.png").sortOrder(0).build()
        ));

        mockMvc.perform(get("/api/shops/VESTIS/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", is("Collection Pâques")));
    }

    // ── POST /api/shops/{shopId}/items/{shopItemId}/buy ───────────────────────

    @Test
    void buyItem_returns200_whenAuthenticated() throws Exception {
        UserItemDto result = UserItemDto.builder()
                .id(20L).item(buildItemDto(10L)).equipped(false).build();
        when(shopService.buyItem(any(), eq(1L), eq(BuyOption.PEZ))).thenReturn(result);

        mockMvc.perform(post("/api/shops/COUPE_TIFF/items/1/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BuyOptionRequest(BuyOption.PEZ)))
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(20)));
    }

    @Test
    void buyItem_returns401_withoutAuth() throws Exception {
        mockMvc.perform(post("/api/shops/COUPE_TIFF/items/1/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BuyOptionRequest(BuyOption.PEZ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buyItem_withKredsOption_callsService() throws Exception {
        UserItemDto result = UserItemDto.builder()
                .id(20L).item(buildItemDto(10L)).equipped(false).build();
        when(shopService.buyItem(any(), eq(5L), eq(BuyOption.KREDS))).thenReturn(result);

        mockMvc.perform(post("/api/shops/IKEBO/items/5/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BuyOptionRequest(BuyOption.KREDS)))
                        .with(auth()))
                .andExpect(status().isOk());

        verify(shopService).buyItem(any(), eq(5L), eq(BuyOption.KREDS));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RequestPostProcessor auth() {
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), "alice", UserRank.ROLE_USER);
        Authentication token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(token);
    }

    private ItemDto buildItemDto(Long id) {
        return ItemDto.builder()
                .id(id).name("Item " + id).itemType("CLOTHING").subType("HAIRSTYLE")
                .possessable(false).displayImage("img/hair.png")
                .spritePath("assets/hair.json").spriteKey("hair")
                .build();
    }

    private ShopItemDto buildShopItemDto(Long id) {
        return ShopItemDto.builder()
                .id(id).item(buildItemDto(10L))
                .pezPrice(200).kredBonus(0).kredPrice(50)
                .build();
    }
}
