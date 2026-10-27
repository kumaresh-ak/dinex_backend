package com.example.dineX.dashboard;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public HotelDashboard hotel(Long hotelId) {
        Timestamp todayStart = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp weekStart = Timestamp.valueOf(LocalDate.now().minusDays(6).atStartOfDay());

        BigDecimal todayRevenue = jdbc.queryForObject(
                "SELECT COALESCE(SUM(grand_total),0) FROM bills WHERE hotel_id = ? AND status = 'PAID' AND paid_at >= ?",
                BigDecimal.class, hotelId, todayStart);
        long todayBills = count(
                "SELECT COUNT(*) FROM bills WHERE hotel_id = ? AND status = 'PAID' AND paid_at >= ?",
                hotelId, todayStart);
        long unsettled = count(
                "SELECT COUNT(*) FROM bills WHERE hotel_id = ? AND status <> 'PAID'", hotelId);
        long openSessions = count(
                "SELECT COUNT(*) FROM dining_sessions WHERE hotel_id = ? AND status IN " + OPEN_SESSIONS, hotelId);
        long occupied = count(
                "SELECT COUNT(DISTINCT table_id) FROM dining_sessions WHERE hotel_id = ? AND status IN " + OPEN_SESSIONS,
                hotelId);
        long activeTables = count(
                "SELECT COUNT(*) FROM dining_tables WHERE hotel_id = ? AND enabled = TRUE AND deleted_flag = FALSE",
                hotelId);
        long inProgress = count(
                "SELECT COUNT(*) FROM orders WHERE hotel_id = ? AND status IN " + IN_PROGRESS_ORDERS, hotelId);
        long pendingPayments = count(
                "SELECT COUNT(*) FROM payments WHERE hotel_id = ? AND status = 'PENDING'", hotelId);
        Double rating = jdbc.queryForObject(
                "SELECT AVG(rating) FROM feedback WHERE hotel_id = ?", Double.class, hotelId);
        long menuItems = count(
                "SELECT COUNT(*) FROM menu_items WHERE hotel_id = ? AND deleted_flag = FALSE", hotelId);
        long chefs = count(
                "SELECT COUNT(*) FROM users WHERE hotel_id = ? AND role = 'CHEF' AND deleted_flag = FALSE", hotelId);

        List<DailyRevenue> series = jdbc.queryForList(
                "SELECT DATE_FORMAT(paid_at, '%Y-%m-%d') AS day, COALESCE(SUM(grand_total),0) AS total "
                        + "FROM bills WHERE hotel_id = ? AND status = 'PAID' AND paid_at >= ? "
                        + "GROUP BY day ORDER BY day",
                hotelId, weekStart).stream()
                .map(row -> new DailyRevenue((String) row.get("day"), (BigDecimal) row.get("total")))
                .toList();

        return new HotelDashboard(todayRevenue, todayBills, unsettled, openSessions, occupied, activeTables,
                inProgress, pendingPayments, rating, menuItems, chefs, series);
    }

    @Transactional(readOnly = true)
    public PlatformDashboard platform() {
        LocalDate today = LocalDate.now();
        Timestamp todayStart = Timestamp.valueOf(today.atStartOfDay());
        Timestamp monthStart = Timestamp.valueOf(today.withDayOfMonth(1).atStartOfDay());

        long totalHotels = count("SELECT COUNT(*) FROM hotels WHERE deleted_flag = FALSE");
        long enabledHotels = count("SELECT COUNT(*) FROM hotels WHERE deleted_flag = FALSE AND enabled = TRUE");
        long activeSubscriptions = count(
                "SELECT COUNT(DISTINCT hotel_id) FROM hotel_subscriptions WHERE status = 'ACTIVE' AND end_date >= ?",
                today);
        long pendingApprovals = count(
                "SELECT COUNT(*) FROM hotel_subscriptions WHERE status = 'PENDING_APPROVAL'");
        long expiring = count(
                "SELECT COUNT(*) FROM hotel_subscriptions WHERE status = 'ACTIVE' AND end_date BETWEEN ? AND ?",
                today, today.plusDays(30));
        BigDecimal subscriptionRevenue = jdbc.queryForObject(
                "SELECT COALESCE(SUM(sp.price),0) FROM hotel_subscriptions hs "
                        + "JOIN subscription_plans sp ON sp.id = hs.plan_id "
                        + "WHERE hs.status IN ('ACTIVE','EXPIRED') AND hs.approved_date >= ?",
                BigDecimal.class, monthStart);
        BigDecimal tenantRevenue = jdbc.queryForObject(
                "SELECT COALESCE(SUM(grand_total),0) FROM bills WHERE status = 'PAID' AND paid_at >= ?",
                BigDecimal.class, todayStart);
        long tenantBills = count(
                "SELECT COUNT(*) FROM bills WHERE status = 'PAID' AND paid_at >= ?", todayStart);
        long staff = count(
                "SELECT COUNT(*) FROM users WHERE hotel_id IS NOT NULL AND deleted_flag = FALSE");

        return new PlatformDashboard(totalHotels, enabledHotels, activeSubscriptions, pendingApprovals, expiring,
                subscriptionRevenue, tenantRevenue, tenantBills, staff);
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}
