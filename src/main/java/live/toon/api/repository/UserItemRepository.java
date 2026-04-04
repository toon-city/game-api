package live.toon.api.repository;

import live.toon.api.entity.Item;
import live.toon.api.entity.ItemSubType;
import live.toon.api.entity.User;
import live.toon.api.entity.UserItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    /**
     * Items disponibles dans l'inventaire : non équipés ET non placés.
     * On eager-fetch l'item associé pour éviter le N+1.
     */
    @Query("""
        SELECT ui FROM UserItem ui
        JOIN FETCH ui.item i
        WHERE ui.user = :user
          AND ui.equipped = false
          AND ui.placedInRoom IS NULL
        """)
    Page<UserItem> findInventory(@Param("user") User user, Pageable pageable);

    /**
     * Items disponibles dans l'inventaire filtrés par type d'item.
     */
    @Query("""
        SELECT ui FROM UserItem ui
        JOIN FETCH ui.item i
        WHERE ui.user = :user
          AND i.itemType = :itemType
          AND ui.equipped = false
          AND ui.placedInRoom IS NULL
        """)
    Page<UserItem> findInventoryByType(
            @Param("user") User user,
            @Param("itemType") live.toon.api.entity.ItemType itemType,
            Pageable pageable);

    /**
     * Trouve l'item actuellement équipé d'un sous-type donné.
     * Utilisé pour déséquiper automatiquement l'ancien avant d'équiper le nouveau.
     */
    @Query("""
        SELECT ui FROM UserItem ui
        JOIN FETCH ui.item i
        WHERE ui.user = :user
          AND i.subType = :subType
          AND ui.equipped = true
        """)
    List<UserItem> findEquippedBySubType(
            @Param("user") User user,
            @Param("subType") ItemSubType subType);

    /** Vérifie qu'un UserItem appartient bien à l'utilisateur donné. */
    Optional<UserItem> findByIdAndUser(Long id, User user);

    /** Trouve une ligne user_items pour un utilisateur et un item donné (si elle existe). */
    Optional<UserItem> findByUserAndItem(User user, Item item);

    /** Tous les items équipés d'un utilisateur (pour reconstruction de l'avatar). */
    @Query("""
        SELECT ui FROM UserItem ui
        JOIN FETCH ui.item i
        WHERE ui.user = :user AND ui.equipped = true
        """)
    List<UserItem> findAllEquipped(@Param("user") User user);
}
