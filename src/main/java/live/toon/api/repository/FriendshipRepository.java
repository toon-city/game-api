package live.toon.api.repository;

import live.toon.api.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** Every friendship touching this user, on either side of the canonical pair. */
    @Query("SELECT f FROM Friendship f WHERE f.userAId = :userId OR f.userBId = :userId")
    List<Friendship> findAllInvolving(@Param("userId") UUID userId);

    /** userAId/userBId must already be canonically ordered (a < b) by the caller. */
    Optional<Friendship> findByUserAIdAndUserBId(UUID userAId, UUID userBId);
}
