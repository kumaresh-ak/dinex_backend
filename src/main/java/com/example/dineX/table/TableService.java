package com.example.dineX.table;

import com.example.dineX.audit.AuditService;
import com.example.dineX.common.ApiException;
import com.example.dineX.config.AppProperties;
import com.example.dineX.order.DiningSession;
import com.example.dineX.order.DiningSessionRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableService {
    private final DiningTableRepository tables;
    private final DiningSessionRepository sessions;
    private final AppProperties properties;
    private final AuditService audit;

    public TableService(DiningTableRepository tables, DiningSessionRepository sessions,
                        AppProperties properties, AuditService audit) {
        this.tables = tables;
        this.sessions = sessions;
        this.properties = properties;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TableDtos.TableView> list(Long hotelId) {
        return tables.findByHotelIdAndDeletedFlagFalseOrderByTableNumberAsc(hotelId).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public TableDtos.TableView create(Long hotelId, TableDtos.TableRequest request) {
        String number = request.tableNumber().trim();
        if (tables.existsByHotelIdAndTableNumber(hotelId, number)) {
            throw ApiException.conflict("A table with this number already exists");
        }
        DiningTable table = new DiningTable();
        table.setHotelId(hotelId);
        table.setTableNumber(number);
        table.setTableName(blankToNull(request.tableName()));
        table.setCapacity(request.capacity());
        table.setEnabled(request.enabled());
        assignQr(table);
        tables.save(table);
        audit.record(hotelId, "TABLE_CREATED", "DiningTable", table.getId(), number);
        return view(table);
    }

    @Transactional
    public TableDtos.TableView update(Long hotelId, Long id, TableDtos.TableRequest request) {
        DiningTable table = find(hotelId, id);
        String number = request.tableNumber().trim();
        if (!number.equals(table.getTableNumber()) && tables.existsByHotelIdAndTableNumber(hotelId, number)) {
            throw ApiException.conflict("A table with this number already exists");
        }
        table.setTableNumber(number);
        table.setTableName(blankToNull(request.tableName()));
        table.setCapacity(request.capacity());
        table.setEnabled(request.enabled());
        tables.save(table);
        audit.record(hotelId, "TABLE_UPDATED", "DiningTable", id, number);
        return view(table);
    }

    @Transactional
    public void delete(Long hotelId, Long id) {
        DiningTable table = find(hotelId, id);
        if (sessions.existsByTableIdAndStatusNot(id, DiningSession.Status.CLOSED)) {
            throw ApiException.conflict("This table has an open session and cannot be removed");
        }
        String number = table.getTableNumber();
        String archived = (number.length() > 18 ? number.substring(0, 18) : number) + "~" + id;
        table.setTableNumber(archived);
        table.setEnabled(false);
        table.setDeletedFlag(true);
        table.setQrToken(null);
        tables.save(table);
        audit.record(hotelId, "TABLE_DELETED", "DiningTable", id, number);
    }

    @Transactional
    public TableDtos.TableView regenerateQr(Long hotelId, Long id) {
        DiningTable table = find(hotelId, id);
        assignQr(table);
        tables.save(table);
        audit.record(hotelId, "TABLE_QR_REGENERATED", "DiningTable", id, table.getTableNumber());
        return view(table);
    }

    @Transactional(readOnly = true)
    public byte[] qrPng(Long hotelId, Long id) {
        DiningTable table = find(hotelId, id);
        return render(table.getQrUrl());
    }

    @Transactional(readOnly = true)
    public DiningTable findByQrToken(String qrToken) {
        return tables.findByQrTokenAndEnabledTrueAndDeletedFlagFalse(qrToken)
                .orElseThrow(() -> ApiException.notFound("Table"));
    }

    private DiningTable find(Long hotelId, Long id) {
        return tables.findByIdAndHotelIdAndDeletedFlagFalse(id, hotelId)
                .orElseThrow(() -> ApiException.notFound("Table"));
    }

    private void assignQr(DiningTable table) {
        String token = UUID.randomUUID().toString().replace("-", "");
        table.setQrToken(token);
        table.setQrUrl(properties.publicBaseUrl().replaceAll("/+$", "") + "/scan/" + token);
    }

    private TableDtos.TableView view(DiningTable table) {
        String activeToken = sessions.findFirstByTableIdAndStatusNotOrderByIdDesc(table.getId(), DiningSession.Status.CLOSED)
                .map(DiningSession::getSessionToken)
                .orElse(null);
        return new TableDtos.TableView(table.getId(), table.getTableNumber(), table.getTableName(), table.getCapacity(),
                table.isEnabled(), table.getQrToken(), table.getQrUrl(), activeToken != null, activeToken);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static byte[] render(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "QR_FAILED", "Unable to render QR code");
        }
    }
}
