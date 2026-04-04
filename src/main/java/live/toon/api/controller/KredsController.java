package live.toon.api.controller;

import live.toon.api.dto.KredsPackageDto;
import live.toon.api.dto.KredsPurchaseRequest;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.KredsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class KredsController {

    private final KredsService kredsService;

    // ─── Public / auth ────────────────────────────────────────────────────────

    @GetMapping("/api/kreds/packages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KredsPackageDto>> listPackages() {
        return ResponseEntity.ok(kredsService.listActivePackages());
    }

    @PostMapping("/api/kreds/purchase")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KredsPackageDto> purchase(
            @AuthenticationPrincipal JwtPrincipal actor,
            @RequestBody KredsPurchaseRequest req) {
        return ResponseEntity.ok(kredsService.purchasePackage(actor, req.packageId()));
    }

    // ─── Admin ────────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/kreds/packages")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Page<KredsPackageDto>> adminListPackages(
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(kredsService.listAllPackages(page));
    }

    @PostMapping("/api/admin/kreds/packages")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<KredsPackageDto> adminCreate(@RequestBody KredsPackageDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kredsService.createPackage(req));
    }

    @PutMapping("/api/admin/kreds/packages/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<KredsPackageDto> adminUpdate(
            @PathVariable Long id,
            @RequestBody KredsPackageDto req) {
        return ResponseEntity.ok(kredsService.updatePackage(id, req));
    }

    @DeleteMapping("/api/admin/kreds/packages/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> adminDelete(@PathVariable Long id) {
        kredsService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}
