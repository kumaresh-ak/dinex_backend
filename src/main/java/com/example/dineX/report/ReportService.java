package com.example.dineX.report;

import com.example.dineX.common.ApiException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant-scoped sales reports. Every query is parameterised for tenant and date range, and only paid bills
 * (settled sessions) contribute revenue.
 */
@Service
public class ReportService {
    private static final long MAX_DAYS = 366;

    public enum Type { SALES, ITEMS, CATEGORY, TABLE, TAX, TIPS, PAYMENTS }

    public enum Period { DAY, WEEK, MONTH, YEAR }

    public record ReportTable(String title, List<String> headers, List<List<Object>> rows) { }

    private final JdbcTemplate jdbc;

    public ReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ReportTable build(Long hotelId, Type type, LocalDate from, LocalDate to, Period period) {
        if (from == null || to == null || to.isBefore(from)) {
            throw ApiException.badRequest("Provide a valid from and to date");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw ApiException.badRequest("The date range cannot exceed 366 days");
        }
        Timestamp start = Timestamp.valueOf(from.atStartOfDay());
        Timestamp end = Timestamp.valueOf(to.plusDays(1).atStartOfDay());
        Period p = period == null ? Period.DAY : period;
        String title = type.name() + " report " + from + " to " + to;

        return switch (type) {
            case SALES -> query(title,
                    List.of("Period", "Bills", "Subtotal", "Tax", "Tips", "Total"),
                    List.of("bucket", "bills", "subtotal", "tax", "tips", "total"),
                    "SELECT " + bucket(p, "b.paid_at") + " AS bucket, COUNT(*) AS bills, "
                            + "COALESCE(SUM(b.subtotal),0) AS subtotal, COALESCE(SUM(b.sgst + b.cgst),0) AS tax, "
                            + "COALESCE(SUM(b.tip_amount),0) AS tips, COALESCE(SUM(b.grand_total),0) AS total "
                            + "FROM bills b WHERE b.hotel_id = ? AND b.status = 'PAID' "
                            + "AND b.paid_at >= ? AND b.paid_at < ? GROUP BY bucket ORDER BY bucket",
                    hotelId, start, end);
            case TAX -> query(title,
                    List.of("Period", "SGST", "CGST", "Total Tax"),
                    List.of("bucket", "sgst", "cgst", "tax"),
                    "SELECT " + bucket(p, "b.paid_at") + " AS bucket, COALESCE(SUM(b.sgst),0) AS sgst, "
                            + "COALESCE(SUM(b.cgst),0) AS cgst, COALESCE(SUM(b.sgst + b.cgst),0) AS tax "
                            + "FROM bills b WHERE b.hotel_id = ? AND b.status = 'PAID' "
                            + "AND b.paid_at >= ? AND b.paid_at < ? GROUP BY bucket ORDER BY bucket",
                    hotelId, start, end);
            case TIPS -> query(title,
                    List.of("Period", "Bills", "Tips"),
                    List.of("bucket", "bills", "tips"),
                    "SELECT " + bucket(p, "b.paid_at") + " AS bucket, COUNT(*) AS bills, "
                            + "COALESCE(SUM(b.tip_amount),0) AS tips "
                            + "FROM bills b WHERE b.hotel_id = ? AND b.status = 'PAID' "
                            + "AND b.paid_at >= ? AND b.paid_at < ? GROUP BY bucket ORDER BY bucket",
                    hotelId, start, end);
            case ITEMS -> query(title,
                    List.of("Item", "Quantity", "Amount"),
                    List.of("item", "quantity", "amount"),
                    "SELECT oi.item_name AS item, SUM(oi.quantity) AS quantity, "
                            + "COALESCE(SUM(oi.total_price),0) AS amount "
                            + "FROM order_items oi JOIN orders o ON o.id = oi.order_id "
                            + "JOIN bills b ON b.session_id = o.session_id AND b.status = 'PAID' "
                            + "WHERE b.hotel_id = ? AND b.paid_at >= ? AND b.paid_at < ? AND o.status <> 'CANCELLED' "
                            + "GROUP BY oi.menu_item_id, oi.item_name ORDER BY amount DESC",
                    hotelId, start, end);
            case CATEGORY -> query(title,
                    List.of("Category", "Quantity", "Amount"),
                    List.of("category", "quantity", "amount"),
                    "SELECT mc.name AS category, SUM(oi.quantity) AS quantity, "
                            + "COALESCE(SUM(oi.total_price),0) AS amount "
                            + "FROM order_items oi JOIN orders o ON o.id = oi.order_id "
                            + "JOIN bills b ON b.session_id = o.session_id AND b.status = 'PAID' "
                            + "JOIN menu_items mi ON mi.id = oi.menu_item_id "
                            + "JOIN menu_categories mc ON mc.id = mi.category_id "
                            + "WHERE b.hotel_id = ? AND b.paid_at >= ? AND b.paid_at < ? AND o.status <> 'CANCELLED' "
                            + "GROUP BY mc.id, mc.name ORDER BY amount DESC",
                    hotelId, start, end);
            case TABLE -> query(title,
                    List.of("Table", "Bills", "Total"),
                    List.of("table", "bills", "total"),
                    "SELECT dt.table_number AS table_no, COUNT(b.id) AS bills, "
                            + "COALESCE(SUM(b.grand_total),0) AS total "
                            + "FROM bills b JOIN dining_sessions ds ON ds.id = b.session_id "
                            + "JOIN dining_tables dt ON dt.id = ds.table_id "
                            + "WHERE b.hotel_id = ? AND b.status = 'PAID' AND b.paid_at >= ? AND b.paid_at < ? "
                            + "GROUP BY dt.id, dt.table_number ORDER BY total DESC",
                    hotelId, start, end);
            case PAYMENTS -> query(title,
                    List.of("Method", "Payments", "Amount"),
                    List.of("method", "payments", "amount"),
                    "SELECT pm.payment_method AS method, COUNT(*) AS payments, "
                            + "COALESCE(SUM(pm.amount),0) AS amount "
                            + "FROM payments pm WHERE pm.hotel_id = ? AND pm.status = 'PAID' "
                            + "AND pm.paid_at >= ? AND pm.paid_at < ? "
                            + "GROUP BY pm.payment_method ORDER BY amount DESC",
                    hotelId, start, end);
        };
    }

    private ReportTable query(String title, List<String> headers, List<String> keys, String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        List<List<Object>> out = rows.stream()
                .map(row -> keys.stream().map(key -> (Object) row.get(key)).toList())
                .toList();
        return new ReportTable(title, headers, out);
    }

    /** Builds the SQL bucket expression for the chosen period. The period is whitelisted by the enum. */
    static String bucket(Period period, String column) {
        return switch (period) {
            case DAY -> "DATE_FORMAT(" + column + ", '%Y-%m-%d')";
            case WEEK -> "DATE_FORMAT(DATE_SUB(" + column + ", INTERVAL WEEKDAY(" + column + ") DAY), '%Y-%m-%d')";
            case MONTH -> "DATE_FORMAT(" + column + ", '%Y-%m')";
            case YEAR -> "DATE_FORMAT(" + column + ", '%Y')";
        };
    }
}
