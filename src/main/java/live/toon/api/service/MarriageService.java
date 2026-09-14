package live.toon.api.service;

import live.toon.api.dto.ConvertPezRequest;
import live.toon.api.dto.JustMarriedDto;
import live.toon.api.dto.MairieStatusDto;
import live.toon.api.dto.MarriageProposalDto;
import live.toon.api.dto.ProposeMarriageRequest;
import live.toon.api.dto.SpouseDto;
import live.toon.api.entity.*;
import live.toon.api.repository.MarriageProposalRepository;
import live.toon.api.repository.UserItemRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mairie: marriage proposals + pez->kred conversion. Two unrelated features
 * sharing one service/controller only because they share one UI panel.
 *
 * Marriage proposals are plain REST, not STOMP — unlike the room-scoped
 * kick/ban/private-message trio (game-server-java, this session), a
 * proposal has nothing to do with being in a room together and must work
 * even if the recipient is offline; there's no live-session requirement to
 * enforce, so no reason to route this through game-server-java at all.
 */
@Service
@RequiredArgsConstructor
public class MarriageService {

    private static final int PEZ_PER_KRED = 100;

    private final UserRepository userRepository;
    private final UserItemRepository userItemRepository;
    private final MarriageProposalRepository marriageProposalRepository;

    @Transactional(readOnly = true)
    public MairieStatusDto status(JwtPrincipal actor) {
        User user = userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        List<MarriageProposal> sent = marriageProposalRepository
                .findByFromUserIdAndStatus(actor.getUserId(), MarriageProposalStatus.PENDING);
        List<MarriageProposal> received = marriageProposalRepository
                .findByToUserIdAndStatus(actor.getUserId(), MarriageProposalStatus.PENDING);

        Set<UUID> otherIds = new HashSet<>();
        sent.forEach(p -> otherIds.add(p.getToUserId()));
        received.forEach(p -> otherIds.add(p.getFromUserId()));
        Map<UUID, String> usernames = userRepository.findAllById(otherIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return MairieStatusDto.builder()
                .marriedToUsername(user.getMarriedTo() != null ? user.getMarriedTo().getUsername() : null)
                .sentProposals(sent.stream().map(p -> toDto(p, p.getToUserId(), usernames)).toList())
                .receivedProposals(received.stream().map(p -> toDto(p, p.getFromUserId(), usernames)).toList())
                .build();
    }

    @Transactional
    public void propose(JwtPrincipal actor, ProposeMarriageRequest req) {
        if (req.toUserId() == null || req.ringUserItemId() == null) {
            throw new IllegalArgumentException("Destinataire et bague requis");
        }
        if (req.toUserId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("Impossible de se marier avec soi-même");
        }

        User actorUser = userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        if (actorUser.getMarriedTo() != null) {
            throw new IllegalArgumentException("Vous êtes déjà marié");
        }

        User target = userRepository.findById(req.toUserId())
                .orElseThrow(() -> new IllegalArgumentException("Toon introuvable"));
        if (target.getMarriedTo() != null) {
            throw new IllegalArgumentException("Ce toon est déjà marié");
        }

        UserItem ring = userItemRepository.findByIdAndUser(req.ringUserItemId(), actorUser)
                .orElseThrow(() -> new IllegalArgumentException("Bague introuvable dans votre inventaire"));
        if (ring.getItem().getSubType() != ItemSubType.RING) {
            throw new IllegalArgumentException("Cet objet n'est pas une bague");
        }
        if (marriageProposalRepository
                .findByRingUserItemIdAndStatus(ring.getId(), MarriageProposalStatus.PENDING).isPresent()) {
            throw new IllegalArgumentException("Cette bague est déjà engagée dans une demande en attente");
        }

        marriageProposalRepository.save(MarriageProposal.builder()
                .fromUserId(actor.getUserId())
                .toUserId(req.toUserId())
                .ringUserItem(ring)
                .build());
    }

    @Transactional
    public void respond(JwtPrincipal actor, Long proposalId, boolean accept) {
        MarriageProposal proposal = marriageProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        if (!proposal.getToUserId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("Cette demande ne vous est pas adressée");
        }
        if (proposal.getStatus() != MarriageProposalStatus.PENDING) {
            throw new IllegalArgumentException("Cette demande n'est plus en attente");
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (!accept) {
            proposal.setStatus(MarriageProposalStatus.DECLINED);
            proposal.setResolvedAt(now);
            marriageProposalRepository.save(proposal);
            return;
        }

        UUID fromId = proposal.getFromUserId();
        UUID toId = proposal.getToUserId();

        // Lock both rows in a stable (id-sorted) order — two proposals
        // accepted concurrently for the same pair (e.g. A->B and B->A, both
        // clicked at once) must not deadlock on each other's row locks.
        boolean fromFirst = fromId.compareTo(toId) <= 0;
        User first  = userRepository.findByIdForUpdate(fromFirst ? fromId : toId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        User second = userRepository.findByIdForUpdate(fromFirst ? toId : fromId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        User fromUser = fromFirst ? first : second;
        User toUser   = fromFirst ? second : first;

        if (fromUser.getMarriedTo() != null || toUser.getMarriedTo() != null) {
            throw new IllegalArgumentException("L'un des deux toons est déjà marié");
        }

        fromUser.setMarriedTo(toUser);
        fromUser.setMarriedAt(now);
        toUser.setMarriedTo(fromUser);
        toUser.setMarriedAt(now);
        userRepository.save(fromUser);
        userRepository.save(toUser);

        UserItem ring = proposal.getRingUserItem();
        if (!ring.getUser().getId().equals(fromId)) {
            // Defensive only — nothing in this codebase lets a ring change
            // hands outside this flow (not equippable, not placeable).
            throw new IllegalArgumentException("La bague ne correspond plus à cette demande");
        }
        ring.setUser(toUser);
        userItemRepository.save(ring);

        proposal.setStatus(MarriageProposalStatus.ACCEPTED);
        proposal.setResolvedAt(now);
        marriageProposalRepository.save(proposal);

        List<MarriageProposal> others = marriageProposalRepository
                .findAllPendingInvolvingAny(List.of(fromId, toId));
        for (MarriageProposal p : others) {
            if (p.getId().equals(proposal.getId())) continue;
            p.setStatus(MarriageProposalStatus.DECLINED);
            p.setResolvedAt(now);
        }
        marriageProposalRepository.saveAll(others);
    }

    @Transactional
    public void cancel(JwtPrincipal actor, Long proposalId) {
        MarriageProposal proposal = marriageProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        if (!proposal.getFromUserId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("Vous ne pouvez annuler que vos propres demandes");
        }
        if (proposal.getStatus() != MarriageProposalStatus.PENDING) {
            throw new IllegalArgumentException("Cette demande n'est plus en attente");
        }
        proposal.setStatus(MarriageProposalStatus.CANCELLED);
        proposal.setResolvedAt(OffsetDateTime.now());
        marriageProposalRepository.save(proposal);
    }

    @Transactional
    public void convertPezToKred(JwtPrincipal actor, ConvertPezRequest req) {
        int pezAmount = req.pezAmount();
        if (pezAmount <= 0 || pezAmount % PEZ_PER_KRED != 0) {
            throw new IllegalArgumentException("Le montant doit être un multiple positif de " + PEZ_PER_KRED + " pez");
        }
        User user = userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        if (user.getPez() < pezAmount) {
            throw new IllegalArgumentException("Pez insuffisants");
        }
        user.setPez(user.getPez() - pezAmount);
        user.setKreds(user.getKreds() + pezAmount / PEZ_PER_KRED);
        userRepository.save(user);
    }

    /** Most recently accepted marriage site-wide — for the home page "just married" panel. */
    @Transactional(readOnly = true)
    public Optional<JustMarriedDto> lastMarried() {
        return marriageProposalRepository.findFirstByStatusOrderByResolvedAtDesc(MarriageProposalStatus.ACCEPTED)
                .map(p -> {
                    User u1 = userRepository.findById(p.getFromUserId()).orElse(null);
                    User u2 = userRepository.findById(p.getToUserId()).orElse(null);
                    if (u1 == null || u2 == null) return null;
                    return JustMarriedDto.builder()
                            .spouse1(toSpouseDto(u1))
                            .spouse2(toSpouseDto(u2))
                            .marriedAt(p.getResolvedAt())
                            .build();
                });
    }

    private SpouseDto toSpouseDto(User user) {
        Map<String, String> clothing = userItemRepository.findAllEquipped(user).stream()
                .map(UserItem::getItem)
                .filter(item -> item.getSpriteKey() != null && item.getSpritePath() != null)
                .collect(Collectors.toMap(Item::getSpriteKey, Item::getSpritePath, (a, b) -> a));
        return SpouseDto.builder()
                .username(user.getUsername())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .skinColor(user.getSkinColor())
                .clothing(clothing)
                .build();
    }

    private MarriageProposalDto toDto(MarriageProposal p, UUID otherUserId, Map<UUID, String> usernames) {
        return MarriageProposalDto.builder()
                .id(p.getId())
                .otherUserId(otherUserId)
                .otherUsername(usernames.get(otherUserId))
                .createdAt(p.getCreatedAt())
                .build();
    }
}
