package live.toon.api.service;

import live.toon.api.dto.DashboardDto;
import live.toon.api.dto.TimeSeriesDto;
import live.toon.api.dto.TopItemDto;
import live.toon.api.dto.TopUserDto;
import live.toon.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PurchaseLogRepository purchaseLogRepository;
    private final KredsPurchaseRepository kredsPurchaseRepository;
    private final ConnectionLogRepository connectionLogRepository;
    private final DeditoonPurchaseLogRepository deditoonPurchaseLogRepository;
    private final JdbcTemplate jdbc;

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        OffsetDateTime todayStart = OffsetDateTime.now().toLocalDate().atStartOfDay()
                .atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime monthStart = todayStart.withDayOfMonth(1);

        long totalUsers        = userRepository.count();
        long onlineNow         = userRepository.countByOnlineTrue();
        long bannedUsers       = userRepository.countByBannedTrue();
        long totalRooms        = roomRepository.count();
        long purchasesToday    = purchaseLogRepository.countByPurchasedAtAfter(todayStart);
        long kredsSpentToday   = purchaseLogRepository.sumKredsSpentAfter(todayStart);
        long pezSpentToday     = purchaseLogRepository.sumPezSpentAfter(todayStart);
        long kreditPurchasesToday = kredsPurchaseRepository.countByPurchasedAtAfter(todayStart);
        long revenueTodayCents = kredsPurchaseRepository.sumRevenueCentsAfter(todayStart);
        long newUsersToday     = connectionLogRepository.countDistinctUsersSince(todayStart);
        long newUsersThisMonth = connectionLogRepository.countDistinctUsersSince(monthStart);

        return DashboardDto.builder()
                .totalUsers(totalUsers)
                .onlineNow(onlineNow)
                .bannedUsers(bannedUsers)
                .totalRooms(totalRooms)
                .purchasesToday(purchasesToday)
                .pezSpentToday(pezSpentToday)
                .kredsSpentOnItemsToday(kredsSpentToday)
                .kredsPurchasesToday(kreditPurchasesToday)
                .revenueTodayCents(revenueTodayCents)
                .dauToday(newUsersToday)
                .mauThisMonth(newUsersThisMonth)
                .build();
    }

    // ─── Time Series ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TimeSeriesDto getTimeSeries(String metric, OffsetDateTime from, OffsetDateTime to, String granularity) {
        String trunc = switch (granularity.toLowerCase()) {
            case "week"  -> "week";
            case "month" -> "month";
            default      -> "day";
        };

        List<TimeSeriesDto.Point> points = switch (metric.toUpperCase()) {
            case "REGISTRATIONS" -> queryTimeSeries(
                    "SELECT date_trunc(?, created_at)::date AS ts, count(*) FROM users WHERE created_at BETWEEN ? AND ? GROUP BY 1 ORDER BY 1",
                    trunc, from, to);
            case "CONNECTIONS" -> queryTimeSeries(
                    "SELECT date_trunc(?, connected_at)::date AS ts, count(*) FROM connection_logs WHERE connected_at BETWEEN ? AND ? GROUP BY 1 ORDER BY 1",
                    trunc, from, to);
            case "PURCHASES" -> queryTimeSeries(
                    "SELECT date_trunc(?, purchased_at)::date AS ts, count(*) FROM purchase_logs WHERE purchased_at BETWEEN ? AND ? GROUP BY 1 ORDER BY 1",
                    trunc, from, to);
            case "KREDS" -> queryTimeSeries(
                    "SELECT date_trunc(?, purchased_at)::date AS ts, count(*) FROM kreds_purchases WHERE purchased_at BETWEEN ? AND ? GROUP BY 1 ORDER BY 1",
                    trunc, from, to);
            case "DEDITOONS" -> queryTimeSeries(
                    "SELECT date_trunc(?, purchased_at)::date AS ts, count(*) FROM deditoon_purchase_logs WHERE purchased_at BETWEEN ? AND ? GROUP BY 1 ORDER BY 1",
                    trunc, from, to);
            default -> throw new IllegalArgumentException("Métrique inconnue : " + metric);
        };

        return TimeSeriesDto.builder()
                .metric(metric)
                .granularity(granularity)
                .points(points)
                .build();
    }

    // ─── Top items ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TopItemDto> getTopItems(int limit, OffsetDateTime from, OffsetDateTime to) {
        String sql = """
            SELECT i.id, i.name, i.display_image, COUNT(*) AS purchase_count
            FROM purchase_logs pl
            JOIN items i ON i.id = pl.item_id
            WHERE pl.purchased_at BETWEEN ? AND ?
            GROUP BY i.id, i.name, i.display_image
            ORDER BY purchase_count DESC
            LIMIT ?
            """;
        return jdbc.query(sql,
                (rs, row) -> TopItemDto.builder()
                        .itemId(rs.getLong("id"))
                        .name(rs.getString("name"))
                        .displayImage(rs.getString("display_image"))
                        .purchaseCount(rs.getLong("purchase_count"))
                        .build(),
                from, to, limit);
    }

    // ─── Top users ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TopUserDto> getTopUsers(int limit, OffsetDateTime from, OffsetDateTime to) {
        String sql = """
            SELECT u.id::text, u.username, COUNT(*) AS purchase_count, COALESCE(SUM(pl.pez_spent), 0) AS total_pez
            FROM purchase_logs pl
            JOIN users u ON u.id = pl.user_id
            WHERE pl.purchased_at BETWEEN ? AND ?
            GROUP BY u.id, u.username
            ORDER BY purchase_count DESC
            LIMIT ?
            """;
        return jdbc.query(sql,
                (rs, row) -> TopUserDto.builder()
                        .userId(java.util.UUID.fromString(rs.getString("id")))
                        .username(rs.getString("username"))
                        .purchaseCount(rs.getLong("purchase_count"))
                        .totalPez(rs.getLong("total_pez"))
                        .build(),
                from, to, limit);
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    private List<TimeSeriesDto.Point> queryTimeSeries(String sql, Object... args) {
        return jdbc.query(sql,
                (rs, row) -> TimeSeriesDto.Point.builder()
                        .period(rs.getString("ts"))
                        .count(rs.getLong(2))
                        .build(),
                args);
    }
}
