package live.toon.api.repository;

import live.toon.api.entity.ItemCollection;
import live.toon.api.entity.ShopId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionRepository extends JpaRepository<ItemCollection, Long> {

    List<ItemCollection> findByShopIdAndEnabledTrueOrderBySortOrderAsc(ShopId shopId);
    List<ItemCollection> findAllByShopIdOrderBySortOrderAsc(ShopId shopId);
}
