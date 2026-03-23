package live.toon.api.repository;

import live.toon.api.entity.Room;
import live.toon.api.entity.RoomType;
import live.toon.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByType(RoomType type);

    List<Room> findAllByOwner(User owner);

    /**
     * Paginates rooms of a given type.
     * When {@code search} is blank the query only returns rooms with userCount > 0 (if {@code peoplOnly = true}).
     * Sorted by userCount DESC then name ASC.
     */
    @Query("""
        SELECT r FROM Room r
        WHERE r.type = :type
          AND (:search = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:peopleOnly = false OR r.userCount > 0)
        ORDER BY r.userCount DESC, r.name ASC
    """)
    Page<Room> findByTypeFiltered(
        @Param("type") RoomType type,
        @Param("search") String search,
        @Param("peopleOnly") boolean peopleOnly,
        Pageable pageable
    );

    /**
     * Paginates private rooms (houses).
     * When {@code search} is blank, only rooms with userCount > 0 are returned (if {@code peopleOnly = true}).
     * Sorted by userCount DESC then name ASC.
     */
    @Query("""
        SELECT r FROM Room r
        WHERE r.type = 'PRIVATE'
          AND (:search = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(r.owner.username) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:peopleOnly = false OR r.userCount > 0)
        ORDER BY r.userCount DESC, r.name ASC
    """)
    Page<Room> findHousesFiltered(
        @Param("search") String search,
        @Param("peopleOnly") boolean peopleOnly,
        Pageable pageable
    );

    /**
     * Paginates private rooms owned by a specific user.
     * Supports optional search by name. Sorted by userCount DESC then name ASC.
     */
    @Query("""
        SELECT r FROM Room r
        WHERE r.type = 'PRIVATE'
          AND r.owner.id = :ownerId
          AND (:search = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY r.userCount DESC, r.name ASC
    """)
    Page<Room> findMyHousesPaged(
        @Param("ownerId") UUID ownerId,
        @Param("search") String search,
        Pageable pageable
    );
}
