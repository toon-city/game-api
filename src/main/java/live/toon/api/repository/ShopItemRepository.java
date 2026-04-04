package live.toon.api.repository;

import live.toon.api.entity.ShopId;
import live.toon.api.entity.ShopItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

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
