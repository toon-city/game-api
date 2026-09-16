package live.toon.api.controller;

import live.toon.api.dto.CreateTradeOfferRequest;
import live.toon.api.dto.TradeOfferDto;
import live.toon.api.entity.ItemType;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TradeController {

    private final TradeService tradeService;

    /** Onglet "Échanges" — place de marché publique, sans mes propres offres. */
    @GetMapping
    public ResponseEntity<Page<TradeOfferDto>> market(
            @AuthenticationPrincipal JwtPrincipal actor,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ItemType itemType,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(tradeService.listMarket(actor, search, itemType, sort, page));
    }

    /** Onglet "Mes propositions". */
    @GetMapping("/mine")
    public ResponseEntity<List<TradeOfferDto>> mine(@AuthenticationPrincipal JwtPrincipal actor) {
        return ResponseEntity.ok(tradeService.listMine(actor));
    }

    /** Onglet "Historique". */
    @GetMapping("/history")
    public ResponseEntity<List<TradeOfferDto>> history(@AuthenticationPrincipal JwtPrincipal actor) {
        return ResponseEntity.ok(tradeService.listHistory(actor));
    }

    @PostMapping
    public ResponseEntity<TradeOfferDto> propose(
            @AuthenticationPrincipal JwtPrincipal actor,
            @RequestBody CreateTradeOfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.propose(actor, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal JwtPrincipal actor, @PathVariable Long id) {
        tradeService.cancel(actor, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<TradeOfferDto> accept(@AuthenticationPrincipal JwtPrincipal actor, @PathVariable Long id) {
        return ResponseEntity.ok(tradeService.accept(actor, id));
    }
}
