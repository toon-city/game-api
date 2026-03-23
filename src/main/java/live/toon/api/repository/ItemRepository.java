package live.toon.api.repository;

import live.toon.api.entity.Item;
import live.toon.api.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByItemType(ItemType itemType);
}
