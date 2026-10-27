package com.example.dineX.support;

import com.example.dineX.security.AuthUser;
import com.example.dineX.support.SupportDtos.StatusUpdateRequest;
import com.example.dineX.support.SupportDtos.TicketRequest;
import com.example.dineX.support.SupportDtos.TicketView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

public final class SupportController {
    private SupportController() { }

    @RestController
    @RequestMapping("/api/v1/hotel/support/tickets")
    public static class HotelSupportController {
        private final SupportService support;

        public HotelSupportController(SupportService support) {
            this.support = support;
        }

        @GetMapping
        public List<TicketView> list(@AuthenticationPrincipal AuthUser user) {
            return support.forHotel(user.hotelId());
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public TicketView raise(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody TicketRequest request) {
            return support.raise(user.hotelId(), user.username(), request);
        }
    }

    @RestController
    @RequestMapping("/api/v1/admin/support/tickets")
    public static class AdminSupportController {
        private final SupportService support;

        public AdminSupportController(SupportService support) {
            this.support = support;
        }

        @GetMapping
        public List<TicketView> list(@RequestParam(required = false) SupportTicket.Status status) {
            return support.all(status);
        }

        @PatchMapping("/{id}")
        public TicketView update(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
            return support.updateStatus(id, request);
        }
    }
}
