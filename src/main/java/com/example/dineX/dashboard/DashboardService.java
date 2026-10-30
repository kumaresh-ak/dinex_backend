package com.example.dineX.dashboard;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    public record DailyRevenue(String day, BigDecimal total) { }

    public record HotelDashboard(
            BigDecimal todayRevenue,
            long todayBills,
            long unsettledBills,
            long openSessions,
            long occupiedTables,
            long activeTables,
            long ordersInProgress,
            long pendingPayments,
            Double averageRating,
            long menuItems,
            long chefs,
            List<DailyRevenue> last7Days) { }

    public record PlatformDashboard(
            long totalHotels,
            long enabledHotels,
            long activeSubscriptions,
            long pendingApprovals,
            long expiringWithin30Days,
            BigDecimal subscriptionRevenueThisMonth,
            BigDecimal tenantRevenueToday,
            long tenantBillsToday,
            long totalStaff) { }

    private static final String OPEN_SESSIONS = "('OPEN','BILL_GENERATED','PAYMENT_PENDING')";
    private static final String IN_PROGRESS_ORDERS = "('ORDERED','ACCEPTED','COOKING','READY')";

    /** All hotel scalar metrics in a single round trip, using scalar subqueries. */
    private static final String HOTEL_SCALARS_SQL =
            "SELECT "
            + "(SELECT COALESCE(SUM(grand_total),0) FROM bills WHERE hotel_id = :hotelId AND status = 'PAID' AND paid_at >= :todayStart) AS today_revenue, "
            + "(SELECT COUNT(*) FROM bills WHERE hotel_id = :hotelId AND status = 'PAID' AND paid_at >= :todayStart) AS today_bills, "
            + "(SELECT COUNT(*) FROM bills WHERE hotel_id = :hotelId AND status <> 'PAID') AS unsettled_bills, "
            + "(SELECT COUNT(*) FROM dining_sessions WHERE hotel_id = :hotelId AND status IN " + OPEN_SESSIONS + ") AS open_sessions, "
            + "(SELECT COUNT(DISTINCT table_id) FROM dining_sessions WHERE hotel_id = :hotelId AND status IN " + OPEN_SESSIONS + ") AS occupied_tables, "
            + "(SELECT COUNT(*) FROM dining_tables WHERE hotel_id = :hotelId AND enabled = TRUE AND deleted_flag = FALSE) AS active_tables, "
            + "(SELECT COUNT(*) FROM orders WHERE hotel_id = :hotelId AND status IN " + IN_PROGRESS_ORDERS + ") AS orders_in_progress, "
            + "(SELECT COUNT(*) FROM payments WHERE hotel_id = :hotelId AND status = 'PENDING') AS pending_payments, "
            + "(SELECT AVG(rating) FROM feedback WHERE hotel_id = :hotelId) AS average_rating, "
            + "(SELECT COUNT(*) FROM menu_items WHERE hotel_id = :hotelId AND deleted_flag = FALSE) AS menu_items, "
            + "(SELECT COUNT(*) FROM users WHERE hotel_id = :hotelId AND role = 'CHEF' AND deleted_flag = FALSE) AS chefs";

    private record HotelScalars(BigDecimal todayRevenue, long todayBills, long unsettledBills, long openSessions,
                                long occupiedTables, long activeTables, long ordersInProgress, long pendingPayments,
                                Double averageRating, long menuItems, long chefs) { }

    private record PlatformScalars(long totalHotels, long enabledHotels, long activeSubscriptions,
                                   long pendingApprovals, long expiring, BigDecimal subscriptionRevenue,
                                   BigDecimal tenantRevenue, long tenantBills, long staff) { }

    private static final String PLATFORM_SCALARS_SQL =
            "SELECT "
            + "(SELECT COUNT(*) FROM hotels WHERE deleted_flag = FALSE) AS total_hotels, "
            + "(SELECT COUNT(*) FROM hotels WHERE deleted_flag = FALSE AND enabled = TRUE) AS enabled_hotels, "
            + "(SELECT COUNT(DISTINCT hotel_id) FROM hotel_subscriptions WHERE status = 'ACTIVE' AND end_date >= :today) AS active_subscriptions, "
            + "(SELECT COUNT(*) FROM hotel_subscriptions WHERE status = 'PENDING_APPROVAL') AS pending_approvals, "
            + "(SELECT COUNT(*) FROM hotel_subscriptions WHERE status = 'ACTIVE' AND end_date BETWEEN :today AND :horizon) AS expiring, "
            + "(SELECT COALESCE(SUM(sp.price),0) FROM hotel_subscriptions hs JOIN subscription_plans sp ON sp.id = hs.plan_id "
            + "WHERE hs.status IN ('ACTIVE','EXPIRED') AND hs.approved_date >= :monthStart) AS subscription_revenue, "
            + "(SELECT COALESCE(SUM(grand_total),0) FROM bills WHERE status = 'PAID' AND paid_at >= :todayStart) AS tenant_revenue, "
            + "(SELECT COUNT(*) FROM bills WHERE status = 'PAID' AND paid_at >= :todayStart) AS tenant_bills, "
            + "(SELECT COUNT(*) FROM users WHERE hotel_id IS NOT NULL AND deleted_flag = FALSE) AS staff";

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
    }

    @Transactional(readOnly = true)
    public HotelDashboard hotel(Long hotelId) {
        Timestamp todayStart = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp weekStart = Timestamp.valueOf(LocalDate.now().minusDays(6).atStartOfDay());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("hotelId", hotelId)
                .addValue("todayStart", todayStart);
        HotelScalars s = named.queryForObject(HOTEL_SCALARS_SQL, params, (rs, rowNum) -> new HotelScalars(
                rs.getBigDecimal("today_revenue"),
                rs.getLong("today_bills"),
                rs.getLong("unsettled_bills"),
                rs.getLong("open_sessions"),
                rs.getLong("occupied_tables"),
                rs.getLong("active_tables"),
                rs.getLong("orders_in_progress"),
                rs.getLong("pending_payments"),
                rs.getObject("average_rating") == null ? null : rs.getDouble("average_rating"),
                rs.getLong("menu_items"),
                rs.getLong("chefs")));

        List<DailyRevenue> series = jdbc.queryForList(
                "SELECT DATE_FORMAT(paid_at, '%Y-%m-%d') AS day, COALESCE(SUM(grand_total),0) AS total "
                        + "FROM bills WHERE hotel_id = ? AND status = 'PAID' AND paid_at >= ? "
                        + "GROUP BY day ORDER BY day",
                hotelId, weekStart).stream()
                .map(row -> new DailyRevenue((String) row.get("day"), (BigDecimal) row.get("total")))
                .toList();

        return new HotelDashboard(s.todayRevenue(), s.todayBills(), s.unsettledBills(), s.openSessions(),
                s.occupiedTables(), s.activeTables(), s.ordersInProgress(), s.pendingPayments(),
                s.averageRating(), s.menuItems(), s.chefs(), series);
    }

    @Transactional(readOnly = true)
    public PlatformDashboard platform() {
        LocalDate today = LocalDate.now();
        Timestamp todayStart = Timestamp.valueOf(today.atStartOfDay());
        Timestamp monthStart = Timestamp.valueOf(today.withDayOfMonth(1).atStartOfDay());

        PlatformScalars s = named.queryForObject(PLATFORM_SCALARS_SQL,
                new MapSqlParameterSource()
                        .addValue("today", today)
                        .addValue("horizon", today.plusDays(30))
                        .addValue("todayStart", todayStart)
                        .addValue("monthStart", monthStart),
                (rs, rowNum) -> new PlatformScalars(
                        rs.getLong("total_hotels"),
                        rs.getLong("enabled_hotels"),
                        rs.getLong("active_subscriptions"),
                        rs.getLong("pending_approvals"),
                        rs.getLong("expiring"),
                        rs.getBigDecimal("subscription_revenue"),
                        rs.getBigDecimal("tenant_revenue"),
                        rs.getLong("tenant_bills"),
                        rs.getLong("staff")));

        return new PlatformDashboard(s.totalHotels(), s.enabledHotels(), s.activeSubscriptions(),
                s.pendingApprovals(), s.expiring(), s.subscriptionRevenue(), s.tenantRevenue(),
                s.tenantBills(), s.staff());
    }
}
