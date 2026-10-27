package com.example.dineX.support;

import com.example.dineX.audit.AuditService;
import com.example.dineX.common.ApiException;
import com.example.dineX.hotel.Hotel;
import com.example.dineX.hotel.HotelRepository;
import com.example.dineX.support.SupportDtos.StatusUpdateRequest;
import com.example.dineX.support.SupportDtos.TicketRequest;
import com.example.dineX.support.SupportDtos.TicketView;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportService {
    private final SupportTicketRepository tickets;
    private final HotelRepository hotels;
    private final AuditService audit;

    public SupportService(SupportTicketRepository tickets, HotelRepository hotels, AuditService audit) {
        this.tickets = tickets;
        this.hotels = hotels;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TicketView> forHotel(Long hotelId) {
        return views(tickets.findByHotelIdOrderByIdDesc(hotelId));
    }

    @Transactional
    public TicketView raise(Long hotelId, String username, TicketRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setHotelId(hotelId);
        ticket.setSubject(request.subject().trim());
        ticket.setMessage(request.message().trim());
        ticket.setCreatedBy(username);
        tickets.save(ticket);
        audit.record(hotelId, "SUPPORT_TICKET_RAISED", "SupportTicket", ticket.getId(), ticket.getSubject());
        return views(List.of(ticket)).get(0);
    }

    @Transactional(readOnly = true)
    public List<TicketView> all(SupportTicket.Status status) {
        List<SupportTicket> rows = status == null
                ? tickets.findAllByOrderByIdDesc()
                : tickets.findByStatusOrderByIdDesc(status);
        return views(rows);
    }

    @Transactional
    public TicketView updateStatus(Long id, StatusUpdateRequest request) {
        SupportTicket ticket = tickets.findById(id)
                .orElseThrow(() -> ApiException.notFound("Support ticket"));
        String resolution = blankToNull(request.resolution());
        SupportTicket.Status status = request.status();
        boolean closing = status == SupportTicket.Status.RESOLVED || status == SupportTicket.Status.CLOSED;
        if (closing && resolution == null && ticket.getResolution() == null) {
            throw ApiException.badRequest("Add a resolution before marking the ticket as " + status.name());
        }
        if (resolution != null) {
            ticket.setResolution(resolution);
        }
        ticket.setStatus(status);
        if (closing) {
            if (ticket.getResolvedDate() == null) {
                ticket.setResolvedDate(LocalDateTime.now());
            }
        } else {
            ticket.setResolvedDate(null);
        }
        tickets.save(ticket);
        audit.record(ticket.getHotelId(), "SUPPORT_TICKET_UPDATED", "SupportTicket", id, status.name());
        return views(List.of(ticket)).get(0);
    }

    private List<TicketView> views(List<SupportTicket> rows) {
        Map<Long, String> hotelNames = new HashMap<>();
        List<Long> hotelIds = rows.stream().map(SupportTicket::getHotelId).distinct().toList();
        for (Hotel hotel : hotels.findAllById(hotelIds)) {
            hotelNames.put(hotel.getId(), hotel.getHotelName());
        }
        return rows.stream()
                .map(t -> new TicketView(t.getId(), t.getHotelId(), hotelNames.get(t.getHotelId()),
                        t.getSubject(), t.getMessage(), t.getStatus(), t.getResolution(), t.getCreatedBy(),
                        t.getCreatedDate(), t.getUpdatedDate(), t.getResolvedDate()))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
