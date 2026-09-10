package live.toon.api.repository;

import jakarta.persistence.LockModeType;
import live.toon.api.entity.ShopId;
import live.toon.api.entity.ShopItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

    /**
     * Same lookup as the plain findById, but row-locked (SELECT ... FOR UPDATE)
     * for the purchase path: without this, two concurrent buyers on the last
     * unit of stock can both pass the `stock > 0` check in
     * ShopService.buyItem() before either decrement lands — the DB's
     * `CHECK (stock >= 0)` constraint stops actual corruption (the second
     * commit fails, rolling back that buyer's pez/kreds debit too), but the
     * failure surfaces as a raw SQL exception instead of the friendly
     * "épuisé" error. Locking here serializes the two buyers instead, so the
     * existing stock check in buyItem() sees accurate data and produces the
     * normal error message.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM ShopItem si JOIN FETCH si.item WHERE si.id = :id")
    Optional<ShopItem> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT si FROM ShopItem si
        JOIN FETCH si.item i
        WHERE si.shopId = :shopId
        ORDER BY si.id ASC
        """)
    Page<ShopItem> findAllByShopIdOrderById(@Param("shopId") ShopId shopId, Pageable pageable);

    @Query("""
        SELECT si FROM ShopItem si
        JOIN FETCH si.item i
        WHERE si.shopId = :shopId
          AND si.available = true
          AND (si.stock IS NULL OR si.stock > 0)
          AND (si.collection IS NULL OR si.collection.enabled = true)
        """)
    Page<ShopItem> findByShopIdAndAvailableTrue(
            @Param("shopId") ShopId shopId,
            Pageable pageable);

    @Query("""
        SELECT si FROM ShopItem si
        JOIN FETCH si.item i
        WHERE si.shopId = :shopId
          AND si.available = true
          AND (si.stock IS NULL OR si.stock > 0)
          AND si.collection.id = :collectionId
          AND si.collection.enabled = true
        """)
    Page<ShopItem> findByShopIdAndAvailableTrueAndCollectionId(
            @Param("shopId") ShopId shopId,
            @Param("collectionId") Long collectionId,
            Pageable pageable);
}
