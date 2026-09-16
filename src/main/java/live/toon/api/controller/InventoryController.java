package live.toon.api.controller;

import live.toon.api.dto.UserItemDto;
import live.toon.api.entity.ItemSubType;
import live.toon.api.entity.ItemType;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Liste les items de l'inventaire de l'utilisateur connecté.
     * Optionnellement filtré par type (FURNITURE, CLOTHING, MISC).
     */
    @GetMapping("/api/inventory")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UserItemDto>> listInventory(
            @AuthenticationPrincipal JwtPrincipal actor,
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) ItemSubType subType,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(inventoryService.listItems(actor, type, subType, page));
    }

    /**
     * Vêtements actuellement équipés (spriteKey → spritePath) — pour afficher
     * l'avatar habillé de l'utilisateur connecté sans passer par une room
     * (ex: le badge avatar de la carte d'identité).
     */
    @GetMapping("/api/inventory/equipped")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, String>> getEquipped(
            @AuthenticationPrincipal JwtPrincipal actor) {
        return ResponseEntity.ok(inventoryService.getEquippedClothing(actor));
    }

    /**
     * Équipe un item de vêtement à l'avatar.
     * Déséquipe automatiquement l'éventuel item du même sous-type.
     */
    @PutMapping("/api/inventory/{id}/equip")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserItemDto> equip(
            @AuthenticationPrincipal JwtPrincipal actor,
            @PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.equipItem(actor, id));
    }

    /**
     * Déséquipe un item de l'avatar.
     */
    @PutMapping("/api/inventory/{id}/unequip")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserItemDto> unequip(
            @AuthenticationPrincipal JwtPrincipal actor,
            @PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.unequipItem(actor, id));
    }
}
