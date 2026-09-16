package live.toon.api.controller;

import live.toon.api.dto.TradeOfferDto;
import live.toon.api.entity.TradeOfferStatus;
import live.toon.api.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/trades")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminTradeController {

    private final TradeService tradeService;

    @GetMapping
    public ResponseEntity<Page<TradeOfferDto>> list(
            @RequestParam(required = false) TradeOfferStatus status,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(tradeService.adminList(status, username, page));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        tradeService.adminCancel(id);
        return ResponseEntity.noContent().build();
    }
}
