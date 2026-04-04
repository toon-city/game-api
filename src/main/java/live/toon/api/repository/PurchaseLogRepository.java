package live.toon.api.repository;

import live.toon.api.entity.PurchaseLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PurchaseLogRepository extends JpaRepository<PurchaseLog, Long> {

    Page<PurchaseLog> findByUserIdOrderByPurchasedAtDesc(UUID userId, Pageable pageable);

    long countByPurchasedAtAfter(OffsetDateTime after);

    @Query("SELECT COALESCE(SUM(p.pezSpent), 0) FROM PurchaseLog p WHERE p.purchasedAt > :after")
    long sumPezSpentAfter(@Param("after") OffsetDateTime after);

    @Query("SELECT COALESCE(SUM(p.kredsSpent), 0) FROM PurchaseLog p WHERE p.purchasedAt > :after")
    long sumKredsSpentAfter(@Param("after") OffsetDateTime after);

    /** Séries temporelles : achats par jour/semaine/mois */
    @Query(nativeQuery = true, value = """
        SELECT DATE_TRUNC(:granularity, purchased_at)::date AS period,
               COUNT(*)                                      AS count,
               COALESCE(SUM(pez_spent), 0)                  AS pez_total,
               COALESCE(SUM(kreds_spent), 0)                AS kreds_total
        FROM purchase_logs
        WHERE purchased_at BETWEEN :from AND :to
        GROUP BY 1 ORDER BY 1
        """)
    List<Object[]> timeSeriesPurchases(
            @Param("granularity") String granularity,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    /** Top items */
    @Query(nativeQuery = true, value = """
        SELECT pl.item_id, i.name, i.display_image, COUNT(*) AS purchase_count
        FROM purchase_logs pl
        JOIN items i ON i.id = pl.item_id
        WHERE pl.purchased_at BETWEEN :from AND :to
        GROUP BY pl.item_id, i.name, i.display_image
        ORDER BY purchase_count DESC
        LIMIT :limitCount
        """)
    List<Object[]> topItems(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("limitCount") int limitCount);

    /** Top users par dépenses */
    @Query(nativeQuery = true, value = """
        SELECT pl.user_id, u.username,
               COUNT(*)                       AS purchase_count,
               COALESCE(SUM(pl.pez_spent), 0) AS total_pez,
               COALESCE(SUM(pl.kreds_spent),0) AS total_kreds
        FROM purchase_logs pl
        JOIN users u ON u.id = pl.user_id
        WHERE pl.purchased_at BETWEEN :from AND :to
        GROUP BY pl.user_id, u.username
        ORDER BY total_kreds DESC, total_pez DESC
        LIMIT :limitCount
        """)
    List<Object[]> topUsersBySpending(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("limitCount") int limitCount);
}
