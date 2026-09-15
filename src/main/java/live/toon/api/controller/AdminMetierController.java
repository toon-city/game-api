package live.toon.api.controller;

import live.toon.api.dto.AdminMetierRequest;
import live.toon.api.dto.MetierDto;
import live.toon.api.service.AdminMetierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/metiers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminMetierController {

    private final AdminMetierService adminMetierService;

    @GetMapping
    public ResponseEntity<List<MetierDto>> listMetiers() {
        return ResponseEntity.ok(adminMetierService.listMetiers());
    }

    @PostMapping
    public ResponseEntity<MetierDto> createMetier(@RequestBody AdminMetierRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminMetierService.createMetier(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetierDto> updateMetier(@PathVariable Long id, @RequestBody AdminMetierRequest req) {
        return ResponseEntity.ok(adminMetierService.updateMetier(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMetier(@PathVariable Long id) {
        adminMetierService.deleteMetier(id);
        return ResponseEntity.noContent().build();
    }
}
