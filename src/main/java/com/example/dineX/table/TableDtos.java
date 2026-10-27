package com.example.dineX.table;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TableDtos {
    private TableDtos() { }

    public record TableRequest(
            @NotBlank @Size(max = 30) String tableNumber,
            @Size(max = 100) String tableName,
            @Min(1) @Max(50) int capacity,
            boolean enabled) { }

    public record TableView(Long id, String tableNumber, String tableName, int capacity, boolean enabled,
                            String qrToken, String qrUrl, boolean occupied, String activeSessionToken) { }
}
