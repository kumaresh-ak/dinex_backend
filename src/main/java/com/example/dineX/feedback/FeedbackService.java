package com.example.dineX.feedback;

import com.example.dineX.billing.BillRepository;
import com.example.dineX.common.ApiException;
import com.example.dineX.feedback.FeedbackDtos.FeedbackRequest;
import com.example.dineX.feedback.FeedbackDtos.FeedbackView;
import com.example.dineX.notification.NotificationService;
import com.example.dineX.order.DiningSession;
import com.example.dineX.order.DiningSessionRepository;
import com.example.dineX.order.OrderService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {
    private final FeedbackRepository feedback;
    private final BillRepository bills;
    private final DiningSessionRepository sessions;
    private final OrderService orderService;
    private final NotificationService notifications;

    public FeedbackService(FeedbackRepository feedback, BillRepository bills, DiningSessionRepository sessions,
                           OrderService orderService, NotificationService notifications) {
        this.feedback = feedback;
        this.bills = bills;
        this.sessions = sessions;
        this.orderService = orderService;
        this.notifications = notifications;
    }

    @Transactional
    public void submit(String sessionToken, FeedbackRequest request) {
        DiningSession session = orderService.requireSession(sessionToken);
        if (bills.findBySessionId(session.getId()).isEmpty()) {
            throw ApiException.conflict("Feedback can be shared after the bill is generated");
        }
        if (feedback.existsBySessionId(session.getId())) {
            throw ApiException.conflict("Feedback has already been submitted for this visit");
        }
        Feedback entry = new Feedback();
        entry.setHotelId(session.getHotelId());
        entry.setSessionId(session.getId());
        entry.setRating(request.rating());
        entry.setComments(request.comments() == null || request.comments().isBlank() ? null : request.comments().trim());
        feedback.save(entry);
        if (request.rating() <= 2) {
            notifications.create(session.getHotelId(), "Low rating received",
                    orderService.tableNumber(session) + " rated " + request.rating() + " out of 5");
        }
        orderService.broadcastSession(session);
    }

    @Transactional(readOnly = true)
    public List<FeedbackView> list(Long hotelId) {
        List<Feedback> rows = feedback.findByHotelIdOrderByIdDesc(hotelId);
        Map<Long, DiningSession> sessionMap = sessions.findAllById(
                rows.stream().map(Feedback::getSessionId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(DiningSession::getId, Function.identity()));
        return rows.stream().map(f -> {
            DiningSession session = sessionMap.get(f.getSessionId());
            return new FeedbackView(f.getId(), session.getSessionNumber(), orderService.tableNumber(session),
                    f.getRating(), f.getComments(), f.getCreatedDate());
        }).toList();
    }
}
