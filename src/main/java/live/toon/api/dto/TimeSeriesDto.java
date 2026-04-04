package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TimeSeriesDto {

    @Data
    @Builder
    public static class Point {
        private String period;  // "2026-03-24"
        private long   count;
        private long   extra1;  // pezTotal | uniqueUsers | revenueCents
        private long   extra2;  // kredsTotal
    }

    private String       metric;
    private String       granularity;
    private List<Point>  points;
}
