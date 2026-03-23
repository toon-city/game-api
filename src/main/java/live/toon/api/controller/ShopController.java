package live.toon.api.controller;

import live.toon.api.dto.BuyOptionRequest;
import live.toon.api.dto.CollectionDto;
import live.toon.api.dto.ShopItemDto;
import live.toon.api.dto.UserItemDto;
import live.toon.api.entity.ShopId;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    /**
     * Liste les articles disponibles d'une boutique.
     * Filtrage optionnel par collection (null = tout afficher).
     * Accessible sans authentification (vitrine publique).
     */
    @GetMapping("/api/shops/{shopId}/items")
    public ResponseEntity<Page<ShopItemDto>> listItems(
            @PathVariable ShopId shopId,
            @RequestParam(required = false) Long collectionId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(shopService.listItems(shopId, collectionId, page));
    }

    /**
     * Liste les collections d'une boutique.
     * Accessible sans authentification.
     */
    @GetMapping("/api/shops/{shopId}/collections")
    public ResponseEntity<List<CollectionDto>> listCollections(@PathVariable ShopId shopId) {
        return ResponseEntity.ok(shopService.listCollections(shopId));
    }

    /**
     * Achète un article de la boutique.
     * Requiert un compte authentifié.
     */
    @PostMapping("/api/shops/{shopId}/items/{shopItemId}/buy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserItemDto> buyItem(
            @AuthenticationPrincipal JwtPrincipal actor,
            @PathVariable ShopId shopId,
            @PathVariable Long shopItemId,
            @RequestBody BuyOptionRequest request) {
        return ResponseEntity.ok(shopService.buyItem(actor, shopItemId, request.option()));
    }
}
