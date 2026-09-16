package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.CreateTradeOfferRequest;
import live.toon.api.dto.TradeOfferDto;
import live.toon.api.entity.*;
import live.toon.api.repository.ItemRepository;
import live.toon.api.repository.TradeOfferRepository;
import live.toon.api.repository.UserItemRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Centre d'échange — public "item (+pez) for item-type (+pez)" offers.
 * Same propose/accept/cancel shape as MarriageService, see that class and
 * the plan doc for why: pessimistic id-sorted double-lock on accept,
 * direct UserItem.setUser() ownership transfer, no flag on UserItem for
 * "committed to a pending trade" (encoded via
 * TradeOfferRepository.existsByOfferedUserItemIdAndStatus instead).
 */
@Service
@RequiredArgsConstructor
public class TradeService {

    private static final int PAGE_SIZE = 24;

    private final UserRepository userRepository;
    private final UserItemRepository userItemRepository;
    private final ItemRepository itemRepository;
    private final TradeOfferRepository tradeOfferRepository;

    // ─── Marketplace / mine / history ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TradeOfferDto> listMarket(JwtPrincipal actor, String search, ItemType itemType, String sort, int page) {
        String q = (search != null && !search.isBlank()) ? search.trim() : null;
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, sortFor(sort));
        return tradeOfferRepository.findMarket(actor.getUserId(), q, itemType, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<TradeOfferDto> listMine(JwtPrincipal actor) {
        return tradeOfferRepository.findByOffererIdAndStatus(actor.getUserId(), TradeOfferStatus.OPEN)
                .stream().map(this::toDto).toList();
    }

    /** Cancelled offers I made, plus accepted trades either as the offerer or the one who accepted. */
    @Transactional(readOnly = true)
    public List<TradeOfferDto> listHistory(JwtPrincipal actor) {
        List<TradeOffer> mineResolved = tradeOfferRepository
                .findByOffererIdAndStatusIn(actor.getUserId(), List.of(TradeOfferStatus.ACCEPTED, TradeOfferStatus.CANCELLED));
        List<TradeOffer> acceptedByMe = tradeOfferRepository
                .findByAcceptedByIdAndStatus(actor.getUserId(), TradeOfferStatus.ACCEPTED);

        Map<Long, TradeOffer> byId = new HashMap<>();
        mineResolved.forEach(t -> byId.put(t.getId(), t));
        acceptedByMe.forEach(t -> byId.put(t.getId(), t));

        return byId.values().stream()
                .sorted((a, b) -> b.getResolvedAt().compareTo(a.getResolvedAt()))
                .map(this::toDto)
                .toList();
    }

    private Sort sortFor(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "oldest" -> Sort.by("createdAt").ascending();
            case "pezAsc" -> Sort.by("offeredPez").ascending();
            case "pezDesc" -> Sort.by("offeredPez").descending();
            default -> Sort.by("createdAt").descending();
        };
    }

    // ─── Propose / cancel / accept ──────────────────────────────────────────────

    @Transactional
    public TradeOfferDto propose(JwtPrincipal actor, CreateTradeOfferRequest req) {
        if (req.offeredUserItemId() == null || req.requestedItemId() == null) {
            throw new IllegalArgumentException("Objet proposé et objet demandé requis");
        }
        if (req.offeredPez() < 0 || req.requestedPez() < 0) {
            throw new IllegalArgumentException("Les pez ne peuvent pas être négatifs");
        }

        User actorUser = userRepository.findByIdForUpdate(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        UserItem offered = userItemRepository.findByIdAndUser(req.offeredUserItemId(), actorUser)
                .orElseThrow(() -> new IllegalArgumentException("Objet introuvable dans votre inventaire"));
        if (offered.isEquipped()) {
            throw new IllegalArgumentException("Cet objet est équipé — retirez-le avant de le proposer à l'échange");
        }
        if (tradeOfferRepository.existsByOfferedUserItemIdAndStatus(offered.getId(), TradeOfferStatus.OPEN)) {
            throw new IllegalArgumentException("Cet objet est déjà proposé dans un autre échange");
        }

        Item requestedItem = itemRepository.findById(req.requestedItemId())
                .orElseThrow(() -> new EntityNotFoundException("Article introuvable : " + req.requestedItemId()));

        if (actorUser.getPez() < req.offeredPez()) {
            throw new IllegalArgumentException("Pez insuffisants");
        }

        // Escrow: pez offered are debited now, refunded on cancel, transferred
        // to whoever accepts — same reasoning as the offered item disappearing
        // from the inventory (plan's design decision 1).
        actorUser.setPez(actorUser.getPez() - req.offeredPez());
        userRepository.save(actorUser);

        TradeOffer trade = tradeOfferRepository.save(TradeOffer.builder()
                .offererId(actor.getUserId())
                .offeredUserItem(offered)
                .offeredPez(req.offeredPez())
                .requestedItem(requestedItem)
                .requestedPez(req.requestedPez())
                .build());

        return toDto(trade);
    }

    @Transactional
    public void cancel(JwtPrincipal actor, Long tradeId) {
        TradeOffer trade = requireOpenTrade(tradeId);
        if (!trade.getOffererId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("Vous ne pouvez annuler que vos propres échanges");
        }
        doCancel(trade);
    }

    /** Admin force-cancel — same refund logic, ownership check lifted. */
    @Transactional
    public void adminCancel(Long tradeId) {
        doCancel(requireOpenTrade(tradeId));
    }

    private void doCancel(TradeOffer trade) {
        User offerer = userRepository.findByIdForUpdate(trade.getOffererId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        offerer.setPez(offerer.getPez() + trade.getOfferedPez());
        userRepository.save(offerer);

        trade.setStatus(TradeOfferStatus.CANCELLED);
        trade.setResolvedAt(OffsetDateTime.now());
        tradeOfferRepository.save(trade);
    }

    @Transactional
    public TradeOfferDto accept(JwtPrincipal actor, Long tradeId) {
        TradeOffer trade = requireOpenTrade(tradeId);
        if (trade.getOffererId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("Vous ne pouvez pas accepter votre propre échange");
        }

        UUID offererId = trade.getOffererId();
        UUID acceptorId = actor.getUserId();

        // Same stable id-sorted lock order as MarriageService.respond()'s
        // accept path — a concurrent accept attempt on this same trade (or
        // any other trade touching the same pair) must not deadlock.
        boolean offererFirst = offererId.compareTo(acceptorId) <= 0;
        User first  = userRepository.findByIdForUpdate(offererFirst ? offererId : acceptorId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        User second = userRepository.findByIdForUpdate(offererFirst ? acceptorId : offererId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        User offerer  = offererFirst ? first : second;
        User acceptor = offererFirst ? second : first;

        List<UserItem> available = userItemRepository.findAvailableForTrade(acceptor, trade.getRequestedItem());
        if (available.isEmpty()) {
            throw new IllegalArgumentException("Vous ne possédez pas cet objet dans votre inventaire");
        }
        if (acceptor.getPez() < trade.getRequestedPez()) {
            throw new IllegalArgumentException("Pez insuffisants");
        }

        UserItem acceptorItem = available.get(0);

        acceptor.setPez(acceptor.getPez() - trade.getRequestedPez() + trade.getOfferedPez());
        offerer.setPez(offerer.getPez() + trade.getRequestedPez());
        userRepository.save(acceptor);
        userRepository.save(offerer);

        UserItem offeredItem = trade.getOfferedUserItem();
        offeredItem.setUser(acceptor);
        userItemRepository.save(offeredItem);

        acceptorItem.setUser(offerer);
        userItemRepository.save(acceptorItem);

        trade.setStatus(TradeOfferStatus.ACCEPTED);
        trade.setAcceptedById(acceptorId);
        trade.setAcceptedUserItem(acceptorItem);
        trade.setResolvedAt(OffsetDateTime.now());
        tradeOfferRepository.save(trade);

        return toDto(trade);
    }

    // ─── Admin ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TradeOfferDto> adminList(TradeOfferStatus status, String username, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        String q = (username != null && !username.isBlank()) ? username.trim() : null;

        if (q == null) {
            return (status == null ? tradeOfferRepository.findAll(pageable) : tradeOfferRepository.findByStatus(status, pageable))
                    .map(this::toDto);
        }

        Set<UUID> userIds = new HashSet<>();
        userRepository.findByUsernameContainingIgnoreCase(q, PageRequest.of(0, 50))
                .forEach(u -> userIds.add(u.getId()));
        if (userIds.isEmpty()) {
            return Page.empty(pageable);
        }
        List<UUID> ids = userIds.stream().toList();
        return (status == null
                ? tradeOfferRepository.findByUserIds(ids, pageable)
                : tradeOfferRepository.findByStatusAndUserIds(status, ids, pageable))
                .map(this::toDto);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Locked read — both accept() and cancel() start here so a concurrent
     * pair of operations on the SAME trade (two accepts, or an accept
     * racing a cancel) serialize on this row: whichever acquires the lock
     * first wins, the second genuinely re-reads (not a stale session-cached
     * object) and correctly finds it no longer OPEN.
     */
    private TradeOffer requireOpenTrade(Long tradeId) {
        TradeOffer trade = tradeOfferRepository.findByIdForUpdate(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("Échange introuvable"));
        if (trade.getStatus() != TradeOfferStatus.OPEN) {
            throw new IllegalArgumentException("Cet échange n'est plus disponible");
        }
        return trade;
    }

    private TradeOfferDto toDto(TradeOffer t) {
        String offererUsername = userRepository.findById(t.getOffererId()).map(User::getUsername).orElse(null);
        String acceptedByUsername = t.getAcceptedById() != null
                ? userRepository.findById(t.getAcceptedById()).map(User::getUsername).orElse(null)
                : null;
        return TradeOfferDto.builder()
                .id(t.getId())
                .offererId(t.getOffererId())
                .offererUsername(offererUsername)
                .offeredItem(InventoryService.toItemDto(t.getOfferedUserItem().getItem()))
                .offeredPez(t.getOfferedPez())
                .requestedItem(InventoryService.toItemDto(t.getRequestedItem()))
                .requestedPez(t.getRequestedPez())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt())
                .acceptedByUsername(acceptedByUsername)
                .resolvedAt(t.getResolvedAt())
                .build();
    }
}
