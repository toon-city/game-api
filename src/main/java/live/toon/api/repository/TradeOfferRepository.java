package live.toon.api.repository;

import jakarta.persistence.LockModeType;
import live.toon.api.entity.ItemType;
import live.toon.api.entity.TradeOffer;
import live.toon.api.entity.TradeOfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradeOfferRepository extends JpaRepository<TradeOffer, Long> {

    /** Used both to reject re-offering an already-offered item and to exclude offered items from inventory listing/equip. */
    boolean existsByOfferedUserItemIdAndStatus(Long userItemId, TradeOfferStatus status);

    /**
     * Locked read — accept()/cancel() both start here so a concurrent
     * accept+cancel (or two concurrent accepts) on the same trade serialize
     * on this row instead of racing on a stale in-memory read. Same
     * reasoning as UserRepository.findByIdForUpdate.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TradeOffer t WHERE t.id = :id")
    Optional<TradeOffer> findByIdForUpdate(@Param("id") Long id);

    /**
     * Public marketplace listing — excludes the viewer's own offers (can't
     * accept your own trade), optional search on either side's item name,
     * optional filter on the requested item's type. One @Query with
     * nullable binds rather than an if/else branch tree: the only list
     * here with 3 combinable optional params (see plan's design decision 7).
     */
    @Query("""
        SELECT t FROM TradeOffer t
        JOIN t.offeredUserItem oui
        JOIN oui.item offeredItem
        JOIN t.requestedItem requestedItem
        WHERE t.status = live.toon.api.entity.TradeOfferStatus.OPEN
          AND t.offererId <> :excludeUserId
          AND (:search IS NULL
               OR LOWER(offeredItem.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(requestedItem.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (:itemType IS NULL OR requestedItem.itemType = :itemType)
        """)
    Page<TradeOffer> findMarket(
            @Param("excludeUserId") UUID excludeUserId,
            @Param("search") String search,
            @Param("itemType") ItemType itemType,
            Pageable pageable);

    List<TradeOffer> findByOffererIdAndStatus(UUID offererId, TradeOfferStatus status);

    List<TradeOffer> findByOffererIdAndStatusIn(UUID offererId, List<TradeOfferStatus> statuses);

    List<TradeOffer> findByAcceptedByIdAndStatus(UUID acceptedById, TradeOfferStatus status);

    // ── Admin listing — plain branch convention (AdminUserService's pattern), not the nullable-@Query one above ──

    Page<TradeOffer> findByStatus(TradeOfferStatus status, Pageable pageable);

    @Query("SELECT t FROM TradeOffer t WHERE t.offererId IN :userIds OR t.acceptedById IN :userIds")
    Page<TradeOffer> findByUserIds(@Param("userIds") List<UUID> userIds, Pageable pageable);

    @Query("SELECT t FROM TradeOffer t WHERE t.status = :status AND (t.offererId IN :userIds OR t.acceptedById IN :userIds)")
    Page<TradeOffer> findByStatusAndUserIds(
            @Param("status") TradeOfferStatus status,
            @Param("userIds") List<UUID> userIds,
            Pageable pageable);
}
