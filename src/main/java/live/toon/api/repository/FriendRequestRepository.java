package live.toon.api.repository;

import live.toon.api.entity.FriendRequest;
import live.toon.api.entity.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByToUserIdAndStatus(UUID toUserId, FriendRequestStatus status);

    List<FriendRequest> findByFromUserIdAndStatus(UUID fromUserId, FriendRequestStatus status);

    /** Is there already a PENDING request between these two, in either direction? */
    @Query("""
        SELECT r FROM FriendRequest r
        WHERE r.status = live.toon.api.entity.FriendRequestStatus.PENDING
          AND ((r.fromUserId = :a AND r.toUserId = :b) OR (r.fromUserId = :b AND r.toUserId = :a))
        """)
    Optional<FriendRequest> findPendingBetween(@Param("a") UUID a, @Param("b") UUID b);
}
