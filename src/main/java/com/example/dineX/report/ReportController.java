package com.example.dineX.report;

import com.example.dineX.common.ApiException;
import com.example.dineX.hotel.HotelRepository;
import com.example.dineX.report.ReportService.Period;
import com.example.dineX.report.ReportService.ReportTable;
import com.example.dineX.report.ReportService.Type;
import com.example.dineX.security.AuthUser;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotel/reports")
public class ReportController {
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reports;
    private final ReportExporter exporter;
    private final HotelRepository hotels;

    public ReportController(ReportService reports, ReportExporter exporter, HotelRepository hotels) {
        this.reports = reports;
        this.exporter = exporter;
        this.hotels = hotels;
    }

    @GetMapping("/{type}")
    public ReportTable report(@AuthenticationPrincipal AuthUser user, @PathVariable String type,
                              @RequestParam LocalDate from, @RequestParam LocalDate to,
                              @RequestParam(defaultValue = "DAY") Period period) {
        return reports.build(user.hotelId(), parseType(type), from, to, period);
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthUser user, @PathVariable String type,
                                         @RequestParam LocalDate from, @RequestParam LocalDate to,
                                         @RequestParam(defaultValue = "DAY") Period period,
                                         @RequestParam(defaultValue = "xlsx") String format) {
        Type reportType = parseType(type);
        ReportTable table = reports.build(user.hotelId(), reportType, from, to, period);
        String base = "report-" + reportType.name().toLowerCase(Locale.ROOT) + "-" + from + "-to-" + to;
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "xlsx" -> ResponseEntity.ok()
                    .contentType(XLSX)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + base + ".xlsx\"")
                    .body(exporter.excel(table));
            case "pdf" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + base + ".pdf\"")
                    .body(exporter.pdf(table, hotelName(user.hotelId())));
            default -> throw ApiException.badRequest("Unsupported format. Use xlsx or pdf");
        };
    }

    private String hotelName(Long hotelId) {
        return hotels.findByIdAndDeletedFlagFalse(hotelId).map(h -> h.getHotelName()).orElse("");
    }

    private static Type parseType(String raw) {
        try {
            return Type.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Unknown report type");
        }
    }
}
