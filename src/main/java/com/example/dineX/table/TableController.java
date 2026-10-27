package com.example.dineX.table;

import com.example.dineX.security.AuthUser;
import com.example.dineX.table.TableDtos.TableRequest;
import com.example.dineX.table.TableDtos.TableView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotel/tables")
public class TableController {
    private final TableService tables;

    public TableController(TableService tables) {
        this.tables = tables;
    }

    @GetMapping
    public List<TableView> list(@AuthenticationPrincipal AuthUser user) {
        return tables.list(user.hotelId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TableView create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody TableRequest request) {
        return tables.create(user.hotelId(), request);
    }

    @PutMapping("/{id}")
    public TableView update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                            @Valid @RequestBody TableRequest request) {
        return tables.update(user.hotelId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        tables.delete(user.hotelId(), id);
    }

    @PostMapping("/{id}/regenerate-qr")
    public TableView regenerate(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return tables.regenerateQr(user.hotelId(), id);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .body(tables.qrPng(user.hotelId(), id));
    }
}
