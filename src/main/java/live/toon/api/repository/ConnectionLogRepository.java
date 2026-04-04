package live.toon.api.repository;

import live.toon.api.entity.ConnectionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ConnectionLogRepository extends JpaRepository<ConnectionLog, Long> {

    Page<ConnectionLog> findByUserIdOrderByConnectedAtDesc(UUID userId, Pageable pageable);

    long countByConnectedAtAfter(OffsetDateTime after);

    /** Nombre d'utilisateurs distincts connectés depuis :after (DAU/MAU). */
    @Query("SELECT COUNT(DISTINCT c.userId) FROM ConnectionLog c WHERE c.connectedAt > :after")
    long countDistinctUsersSince(@Param("after") OffsetDateTime after);

    @Query(nativeQuery = true, value = """
        SELECT DATE_TRUNC(:granularity, connected_at)::date AS period,
               COUNT(*)                                     AS count,
               COUNT(DISTINCT user_id)                      AS unique_users
        FROM connection_logs
        WHERE connected_at BETWEEN :from AND :to
        GROUP BY 1 ORDER BY 1
        """)
    List<Object[]> timeSeriesConnections(
            @Param("granularity") String granularity,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
