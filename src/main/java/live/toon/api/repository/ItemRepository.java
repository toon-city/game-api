package live.toon.api.repository;

import live.toon.api.entity.Item;
import live.toon.api.entity.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByItemType(ItemType itemType);

    /** Backs the admin catalogue's "Rechercher" box + type filter — see AdminItemService.listItems. */
    Page<Item> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Item> findByItemType(ItemType itemType, Pageable pageable);

    Page<Item> findByNameContainingIgnoreCaseAndItemType(String name, ItemType itemType, Pageable pageable);
}
