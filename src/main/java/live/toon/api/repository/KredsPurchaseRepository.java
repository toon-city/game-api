package live.toon.api.repository;

import live.toon.api.entity.KredsPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface KredsPurchaseRepository extends JpaRepository<KredsPurchase, Long> {

    Page<KredsPurchase> findByUserIdOrderByPurchasedAtDesc(UUID userId, Pageable pageable);

    long countByPurchasedAtAfter(OffsetDateTime after);

    @Query("SELECT COALESCE(SUM(k.priceCents), 0) FROM KredsPurchase k WHERE k.purchasedAt > :after")
    long sumRevenueCentsAfter(@Param("after") OffsetDateTime after);

    @Query(nativeQuery = true, value = """
        SELECT DATE_TRUNC(:granularity, purchased_at)::date AS period,
               COUNT(*)                                      AS count,
               COALESCE(SUM(price_cents), 0)                AS revenue_cents
        FROM kreds_purchases
        WHERE purchased_at BETWEEN :from AND :to
        GROUP BY 1 ORDER BY 1
        """)
    List<Object[]> timeSeriesKredsPurchases(
            @Param("granularity") String granularity,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
