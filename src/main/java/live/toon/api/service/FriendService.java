package live.toon.api.service;

import live.toon.api.dto.BlockedUserDto;
import live.toon.api.dto.FriendDto;
import live.toon.api.dto.FriendRequestDto;
import live.toon.api.dto.FriendsStatusDto;
import live.toon.api.entity.FriendRequest;
import live.toon.api.entity.FriendRequestStatus;
import live.toon.api.entity.Friendship;
import live.toon.api.entity.User;
import live.toon.api.entity.UserBlock;
import live.toon.api.entity.UserItem;
import live.toon.api.entity.Item;
import live.toon.api.repository.FriendRequestRepository;
import live.toon.api.repository.FriendshipRepository;
import live.toon.api.repository.UserBlockRepository;
import live.toon.api.repository.UserItemRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Amis / liste noire. Two independent relations sharing one service/panel,
 * same reasoning as MarriageService bundling proposals + pez conversion.
 *
 * A friend request auto-accepts if the other side already has a pending
 * request to you — no need to make two people click twice when they both
 * wanted it. Blocking severs any existing friendship and cancels any
 * pending request between the pair, and — checked in both directions —
 * prevents new requests either way until unblocked.
 */
@Service
@RequiredArgsConstructor
public class FriendService {

    private final UserRepository userRepository;
    private final UserItemRepository userItemRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;

    @Transactional(readOnly = true)
    public FriendsStatusDto status(JwtPrincipal actor) {
        UUID me = actor.getUserId();

        var friendships = friendshipRepository.findAllInvolving(me);
        var sent = friendRequestRepository.findByFromUserIdAndStatus(me, FriendRequestStatus.PENDING);
        var received = friendRequestRepository.findByToUserIdAndStatus(me, FriendRequestStatus.PENDING);
        var blocks = userBlockRepository.findByBlockerId(me);

        Set<UUID> otherIds = new HashSet<>();
        for (Friendship f : friendships) otherIds.add(f.getUserAId().equals(me) ? f.getUserBId() : f.getUserAId());
        sent.forEach(r -> otherIds.add(r.getToUserId()));
        received.forEach(r -> otherIds.add(r.getFromUserId()));
        blocks.forEach(b -> otherIds.add(b.getBlockedId()));
        Map<UUID, User> users = userRepository.findAllById(otherIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        // Equipped-items query per user, memoized across this one status()
        // call — same "loop, one query per user" style as
        // MarriageService.toSpouseDto, fine here since a friends list is
        // naturally small (not a paginated directory like /api/users).
        Map<UUID, Map<String, String>> clothingCache = new HashMap<>();

        return FriendsStatusDto.builder()
                .friends(friendships.stream().map(f -> {
                    UUID otherId = f.getUserAId().equals(me) ? f.getUserBId() : f.getUserAId();
                    User u = users.get(otherId);
                    return FriendDto.builder()
                            .userId(otherId)
                            .username(u != null ? u.getUsername() : null)
                            .since(f.getCreatedAt())
                            .skinColor(u != null ? u.getSkinColor() : null)
                            .clothing(clothingFor(u, clothingCache))
                            .build();
                }).toList())
                .sentRequests(sent.stream().map(r -> toRequestDto(r, r.getToUserId(), users, clothingCache)).toList())
                .receivedRequests(received.stream().map(r -> toRequestDto(r, r.getFromUserId(), users, clothingCache)).toList())
                .blocked(blocks.stream()
                        .map(b -> {
                            User u = users.get(b.getBlockedId());
                            return BlockedUserDto.builder()
                                    .userId(b.getBlockedId())
                                    .username(u != null ? u.getUsername() : null)
                                    .since(b.getCreatedAt())
                                    .skinColor(u != null ? u.getSkinColor() : null)
                                    .clothing(clothingFor(u, clothingCache))
                                    .build();
                        })
                        .toList())
                .build();
    }

    @Transactional
    public void sendRequest(JwtPrincipal actor, UUID targetId) {
        UUID me = actor.getUserId();
        if (targetId == null) throw new IllegalArgumentException("Destinataire requis");
        if (targetId.equals(me)) throw new IllegalArgumentException("Impossible de s'ajouter soi-même");
        userRepository.findById(targetId).orElseThrow(() -> new IllegalArgumentException("Toon introuvable"));

        if (!userBlockRepository.findBetweenEitherDirection(me, targetId).isEmpty()) {
            throw new IllegalArgumentException("Impossible — ce toon est bloqué");
        }
        if (friendshipRepository.findByUserAIdAndUserBId(min(me, targetId), max(me, targetId)).isPresent()) {
            throw new IllegalArgumentException("Vous êtes déjà amis avec ce toon");
        }

        Optional<FriendRequest> existing = friendRequestRepository.findPendingBetween(me, targetId);
        if (existing.isPresent()) {
            FriendRequest req = existing.get();
            if (req.getFromUserId().equals(me)) {
                throw new IllegalArgumentException("Une demande est déjà en attente avec ce toon");
            }
            // The other side already asked you — accept theirs instead of
            // creating a redundant second request.
            acceptInternal(req);
            return;
        }

        friendRequestRepository.save(FriendRequest.builder().fromUserId(me).toUserId(targetId).build());
    }

    @Transactional
    public void respond(JwtPrincipal actor, Long requestId, boolean accept) {
        FriendRequest req = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        if (!req.getToUserId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("Cette demande ne vous est pas adressée");
        }
        if (req.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("Cette demande n'est plus en attente");
        }

        if (accept) {
            acceptInternal(req);
        } else {
            req.setStatus(FriendRequestStatus.DECLINED);
            req.setResolvedAt(OffsetDateTime.now());
            friendRequestRepository.save(req);
        }
    }

    @Transactional
    public void cancel(JwtPrincipal actor, Long requestId) {
        FriendRequest req = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        if (!req.getFromUserId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("Vous ne pouvez annuler que vos propres demandes");
        }
        if (req.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("Cette demande n'est plus en attente");
        }
        req.setStatus(FriendRequestStatus.CANCELLED);
        req.setResolvedAt(OffsetDateTime.now());
        friendRequestRepository.save(req);
    }

    @Transactional
    public void removeFriend(JwtPrincipal actor, UUID friendId) {
        UUID me = actor.getUserId();
        Friendship f = friendshipRepository.findByUserAIdAndUserBId(min(me, friendId), max(me, friendId))
                .orElseThrow(() -> new IllegalArgumentException("Vous n'êtes pas amis avec ce toon"));
        friendshipRepository.delete(f);
    }

    @Transactional
    public void block(JwtPrincipal actor, UUID targetId) {
        UUID me = actor.getUserId();
        if (targetId == null) throw new IllegalArgumentException("Toon requis");
        if (targetId.equals(me)) throw new IllegalArgumentException("Impossible de se bloquer soi-même");
        userRepository.findById(targetId).orElseThrow(() -> new IllegalArgumentException("Toon introuvable"));

        friendshipRepository.findByUserAIdAndUserBId(min(me, targetId), max(me, targetId))
                .ifPresent(friendshipRepository::delete);

        friendRequestRepository.findPendingBetween(me, targetId).ifPresent(r -> {
            r.setStatus(FriendRequestStatus.CANCELLED);
            r.setResolvedAt(OffsetDateTime.now());
            friendRequestRepository.save(r);
        });

        if (userBlockRepository.findByBlockerIdAndBlockedId(me, targetId).isEmpty()) {
            userBlockRepository.save(UserBlock.builder().blockerId(me).blockedId(targetId).build());
        }
    }

    @Transactional
    public void unblock(JwtPrincipal actor, UUID targetId) {
        UserBlock block = userBlockRepository.findByBlockerIdAndBlockedId(actor.getUserId(), targetId)
                .orElseThrow(() -> new IllegalArgumentException("Ce toon n'est pas bloqué"));
        userBlockRepository.delete(block);
    }

    private void acceptInternal(FriendRequest req) {
        OffsetDateTime now = OffsetDateTime.now();
        req.setStatus(FriendRequestStatus.ACCEPTED);
        req.setResolvedAt(now);
        friendRequestRepository.save(req);

        UUID lo = min(req.getFromUserId(), req.getToUserId());
        UUID hi = max(req.getFromUserId(), req.getToUserId());
        if (friendshipRepository.findByUserAIdAndUserBId(lo, hi).isEmpty()) {
            friendshipRepository.save(Friendship.builder().userAId(lo).userBId(hi).build());
        }
    }

    private FriendRequestDto toRequestDto(FriendRequest r, UUID otherUserId, Map<UUID, User> users, Map<UUID, Map<String, String>> clothingCache) {
        User u = users.get(otherUserId);
        return FriendRequestDto.builder()
                .id(r.getId())
                .otherUserId(otherUserId)
                .otherUsername(u != null ? u.getUsername() : null)
                .createdAt(r.getCreatedAt())
                .skinColor(u != null ? u.getSkinColor() : null)
                .clothing(clothingFor(u, clothingCache))
                .build();
    }

    /** spriteKey (catégorie) -> spritePath for this user's currently equipped items — memoized per status() call. */
    private Map<String, String> clothingFor(User user, Map<UUID, Map<String, String>> cache) {
        if (user == null) return Map.of();
        return cache.computeIfAbsent(user.getId(), id -> userItemRepository.findAllEquipped(user).stream()
                .map(UserItem::getItem)
                .filter(item -> item.getSpriteKey() != null && item.getSpritePath() != null)
                .collect(Collectors.toMap(Item::getSpriteKey, Item::getSpritePath, (a, b) -> a)));
    }

    private static UUID min(UUID a, UUID b) { return a.toString().compareTo(b.toString()) <= 0 ? a : b; }
    private static UUID max(UUID a, UUID b) { return a.toString().compareTo(b.toString()) <= 0 ? b : a; }
}
