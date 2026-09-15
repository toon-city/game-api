package live.toon.api.repository;

import live.toon.api.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    List<UserBlock> findByBlockerId(UUID blockerId);

    Optional<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    /** Either direction — a block by either party is enough to sever a pair. */
    @Query("""
        SELECT b FROM UserBlock b
        WHERE (b.blockerId = :a AND b.blockedId = :b) OR (b.blockerId = :b AND b.blockedId = :a)
        """)
    List<UserBlock> findBetweenEitherDirection(@Param("a") UUID a, @Param("b") UUID b);
}
