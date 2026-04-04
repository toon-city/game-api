package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardDto {
    // Utilisateurs
    private long totalUsers;
    private long onlineNow;
    private long bannedUsers;
    private long newUsersToday;
    private long newUsersThisMonth;
    private long dauToday;     // utilisateurs distincts connectés aujourd'hui
    private long mauThisMonth; // utilisateurs distincts connectés ce mois

    // Rooms
    private long totalRooms;
    private long lockedRooms;

    // Achats d'items (aujourd'hui)
    private long purchasesToday;
    private long pezSpentToday;
    private long kredsSpentOnItemsToday;

    // Kreds (aujourd'hui)
    private long kredsPurchasesToday;
    private long revenueTodayCents; // revenus en centimes

    // Déditoons (aujourd'hui)
    private long deditoonPurchasesToday;
}
