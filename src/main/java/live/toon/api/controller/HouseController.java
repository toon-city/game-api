package live.toon.api.controller;

import jakarta.validation.Valid;
import live.toon.api.dto.*;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.HouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;

    // ── Schémas (publics) ─────────────────────────────────────────────────────

    @GetMapping("/api/house-schemas")
    public ResponseEntity<List<HouseSchemaDto>> listSchemas() {
        return ResponseEntity.ok(houseService.listSchemas());
    }

    // ── Maisons privées ───────────────────────────────────────────────────────

    /** Liste toutes les maisons privées (zone privée du lobby) */
    @GetMapping("/api/houses")
    public ResponseEntity<List<HouseDto>> listHouses() {
        return ResponseEntity.ok(houseService.listPrivateHouses());
    }

    /** Mes maisons */
    @GetMapping("/api/houses/mine")
    public ResponseEntity<List<HouseDto>> myHouses(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(houseService.listMyHouses(principal.getUserId()));
    }

    /** Créer une nouvelle maison privée */
    @PostMapping("/api/houses")
    public ResponseEntity<HouseDto> createHouse(
            @Valid @RequestBody HouseRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        HouseDto created = houseService.createHouse(principal.getUserId(), request);
        return ResponseEntity
                .created(URI.create("/api/houses/" + created.getId()))
                .body(created);
    }

    /** Modifier sa maison */
    @PutMapping("/api/houses/{id}")
    public ResponseEntity<HouseDto> updateHouse(
            @PathVariable Long id,
            @Valid @RequestBody HouseRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(houseService.updateHouse(principal.getUserId(), id, request));
    }

    /** Supprimer sa maison */
    @DeleteMapping("/api/houses/{id}")
    public ResponseEntity<Void> deleteHouse(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        houseService.deleteHouse(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Valider l'accès à une maison avant de rejoindre la salle.
     * Retourne 200 OK si l'accès est autorisé, 400 sinon.
     */
    @PostMapping("/api/houses/{id}/enter")
    public ResponseEntity<Void> enterHouse(
            @PathVariable Long id,
            @RequestBody(required = false) EnterHouseRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        String password = request != null ? request.getPassword() : null;
        houseService.validateAccess(principal.getUserId(), id, password);
        return ResponseEntity.ok().build();
    }
}
