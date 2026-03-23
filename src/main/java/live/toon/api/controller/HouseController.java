package live.toon.api.controller;

import jakarta.validation.Valid;
import live.toon.api.dto.*;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.HouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

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

    /** Liste toutes les maisons privées (rétrocompatibilité) */
    @GetMapping("/api/houses")
    public ResponseEntity<List<HouseDto>> listHouses() {
        return ResponseEntity.ok(houseService.listPrivateHouses());
    }

    /**
     * Liste paginée des maisons privées.
     * Sans recherche : uniquement les maisons avec au moins 1 joueur (trié userCount DESC → name ASC).
     * Avec recherche : toutes les maisons correspondantes (par nom ou propriétaire).
     */
    @GetMapping("/api/houses/paged")
    public ResponseEntity<org.springframework.data.domain.Page<HouseDto>> listHousesPaged(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(houseService.listPrivateHousesPaged(q, page, size));
    }

    /** Mes maisons */
    @GetMapping("/api/houses/mine")
    public ResponseEntity<List<HouseDto>> myHouses(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(houseService.listMyHouses(principal.getUserId()));
    }

    /** Mes maisons — paginées et filtrables par nom */
    @GetMapping("/api/houses/mine/paged")
    public ResponseEntity<Page<HouseDto>> myHousesPaged(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(houseService.listMyHousesPaged(principal.getUserId(), q, page, size));
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

    /** Modifier sa maison — autorisé uniquement par le propriétaire ou un admin */
    @PutMapping("/api/houses/{id}")
    @PreAuthorize("hasPermission(#id, 'House', 'update')")
    public ResponseEntity<HouseDto> updateHouse(
            @PathVariable Long id,
            @Valid @RequestBody HouseRequest request) {
        return ResponseEntity.ok(houseService.updateHouse(id, request));
    }

    /** Supprimer sa maison — autorisé uniquement par le propriétaire ou un admin */
    @DeleteMapping("/api/houses/{id}")
    @PreAuthorize("hasPermission(#id, 'House', 'delete')")
    public ResponseEntity<Void> deleteHouse(@PathVariable Long id) {
        houseService.deleteHouse(id);
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
        houseService.validateAccess(principal, id, password);
        return ResponseEntity.ok().build();
    }
}
