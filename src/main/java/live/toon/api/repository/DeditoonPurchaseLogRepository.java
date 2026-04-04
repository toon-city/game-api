package live.toon.api.repository;

import live.toon.api.entity.DeditoonPurchaseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface DeditoonPurchaseLogRepository extends JpaRepository<DeditoonPurchaseLog, Long> {

    long countByPurchasedAtAfter(OffsetDateTime after);

    @Query(nativeQuery = true, value = """
        SELECT DATE_TRUNC(:granularity, purchased_at)::date AS period,
               COUNT(*)                                     AS count,
               COALESCE(SUM(pez_spent), 0)                 AS pez_total
        FROM deditoon_purchase_logs
        WHERE purchased_at BETWEEN :from AND :to
        GROUP BY 1 ORDER BY 1
        """)
    List<Object[]> timeSeriesDeditoonPurchases(
            @Param("granularity") String granularity,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
