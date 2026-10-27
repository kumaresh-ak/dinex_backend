package com.example.dineX.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class SupportDtos {
    private SupportDtos() { }

    public record TicketRequest(
            @NotBlank @Size(max = 160) String subject,
            @NotBlank @Size(max = 2000) String message) { }

    public record StatusUpdateRequest(
            @NotNull SupportTicket.Status status,
            @Size(max = 2000) String resolution) { }

    public record TicketView(Long id, Long hotelId, String hotelName, String subject, String message,
                             SupportTicket.Status status, String resolution, String createdBy,
                             LocalDateTime createdDate, LocalDateTime updatedDate, LocalDateTime resolvedDate) { }
}
