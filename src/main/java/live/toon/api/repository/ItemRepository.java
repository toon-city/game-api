package live.toon.api.repository;

import live.toon.api.entity.Item;
import live.toon.api.entity.ItemSubType;
import live.toon.api.entity.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByItemType(ItemType itemType);

    /** Backs the admin catalogue's "Rechercher" box + type filter — see AdminItemService.listItems. */
    Page<Item> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Item> findByItemType(ItemType itemType, Pageable pageable);

    Page<Item> findByNameContainingIgnoreCaseAndItemType(String name, ItemType itemType, Pageable pageable);

    /**
     * Backs ItemController's public catalogue picker (trade center's "objet
     * proposé/demandé" selects) — 3 combinable optional params, same
     * nullable-@Query reasoning as TradeOfferRepository.findMarket rather
     * than an if/else branch tree for every combination.
     */
    @Query("""
        SELECT i FROM Item i
        WHERE (:search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (:itemType IS NULL OR i.itemType = :itemType)
          AND (:subType IS NULL OR i.subType = :subType)
        """)
    Page<Item> search(
            @Param("search") String search,
            @Param("itemType") ItemType itemType,
            @Param("subType") ItemSubType subType,
            Pageable pageable);
}
