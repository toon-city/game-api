package live.toon.api.controller;

import live.toon.api.dto.DashboardDto;
import live.toon.api.dto.TimeSeriesDto;
import live.toon.api.dto.TopItemDto;
import live.toon.api.dto.TopUserDto;
import live.toon.api.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDto> dashboard() {
        return ResponseEntity.ok(adminStatsService.getDashboard());
    }

    @GetMapping("/series")
    public ResponseEntity<TimeSeriesDto> timeSeries(
            @RequestParam String metric,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "day") String granularity) {
        return ResponseEntity.ok(adminStatsService.getTimeSeries(metric, from, to, granularity));
    }

    @GetMapping("/top-items")
    public ResponseEntity<List<TopItemDto>> topItems(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ResponseEntity.ok(adminStatsService.getTopItems(limit, from, to));
    }

    @GetMapping("/top-users")
    public ResponseEntity<List<TopUserDto>> topUsers(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ResponseEntity.ok(adminStatsService.getTopUsers(limit, from, to));
    }
}
