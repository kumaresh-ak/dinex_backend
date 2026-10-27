package com.example.dineX.audit;

import com.example.dineX.common.ApiException;
import com.example.dineX.hotel.Hotel;
import com.example.dineX.hotel.HotelRepository;
import com.example.dineX.report.ReportExporter;
import com.example.dineX.report.ReportService.ReportTable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Super admin view and export of the platform-wide audit trail. */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditController {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final int VIEW_LIMIT = 500;
    private static final int EXPORT_LIMIT = 5000;

    public record AuditView(Long id, Long hotelId, String hotelName, String username, String action,
                            String entityType, String entityId, String details, LocalDateTime createdDate) { }

    private final AuditLogRepository audit;
    private final HotelRepository hotels;
    private final ReportExporter exporter;

    public AuditController(AuditLogRepository audit, HotelRepository hotels, ReportExporter exporter) {
        this.audit = audit;
        this.hotels = hotels;
        this.exporter = exporter;
    }

    @GetMapping
    public List<AuditView> list(@RequestParam(required = false) Long hotelId,
                                @RequestParam(required = false) String action,
                                @RequestParam(required = false) LocalDate from,
                                @RequestParam(required = false) LocalDate to) {
        return views(search(hotelId, action, from, to, VIEW_LIMIT));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long hotelId,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to,
                                         @RequestParam(defaultValue = "xlsx") String format) {
        List<AuditView> rows = views(search(hotelId, action, from, to, EXPORT_LIMIT));
        List<List<Object>> body = rows.stream()
                .map(r -> Arrays.<Object>asList(
                        r.createdDate() == null ? "" : r.createdDate().format(STAMP),
                        r.hotelName() == null ? "Platform" : r.hotelName(),
                        r.username(),
                        r.action(),
                        r.entityType(),
                        r.entityId() == null ? "" : r.entityId(),
                        r.details() == null ? "" : r.details()))
                .toList();
        ReportTable table = new ReportTable("Audit log",
                List.of("Time", "Hotel", "User", "Action", "Entity", "Entity ID", "Details"), body);
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "xlsx" -> ResponseEntity.ok()
                    .contentType(XLSX)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log.xlsx\"")
                    .body(exporter.excel(table));
            case "pdf" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log.pdf\"")
                    .body(exporter.pdf(table, "DineX platform"));
            default -> throw ApiException.badRequest("Unsupported format. Use xlsx or pdf");
        };
    }

    private List<AuditLog> search(Long hotelId, String action, LocalDate from, LocalDate to, int limit) {
        String cleanAction = action == null || action.isBlank() ? null : action.trim();
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
        return audit.search(hotelId, cleanAction, fromTime, toTime, PageRequest.of(0, limit));
    }

    private List<AuditView> views(List<AuditLog> logs) {
        Map<Long, String> hotelNames = new HashMap<>();
        List<Long> hotelIds = logs.stream().map(AuditLog::getHotelId).filter(id -> id != null).distinct().toList();
        for (Hotel hotel : hotels.findAllById(hotelIds)) {
            hotelNames.put(hotel.getId(), hotel.getHotelName());
        }
        return logs.stream()
                .map(l -> new AuditView(l.getId(), l.getHotelId(), hotelNames.get(l.getHotelId()), l.getUsername(),
                        l.getAction(), l.getEntityType(), l.getEntityId(), l.getDetails(), l.getCreatedDate()))
                .toList();
    }
}
