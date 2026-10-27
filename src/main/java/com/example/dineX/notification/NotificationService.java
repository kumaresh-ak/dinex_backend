package com.example.dineX.notification;

import com.example.dineX.common.ApiException;
import com.example.dineX.realtime.RealtimePublisher;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    public record View(Long id, String title, String message, boolean read, LocalDateTime createdAt) { }

    public record UnreadCount(long unread) { }

    private final NotificationRepository notifications;
    private final RealtimePublisher realtime;

    public NotificationService(NotificationRepository notifications, RealtimePublisher realtime) {
        this.notifications = notifications;
        this.realtime = realtime;
    }

    @Transactional
    public void create(Long hotelId, String title, String message) {
        Notification notification = new Notification();
        notification.setHotelId(hotelId);
        notification.setTitle(title);
        notification.setMessage(message);
        notifications.save(notification);
        realtime.publish(hotelId, null, "NOTIFICATION", view(notification));
    }

    @Transactional(readOnly = true)
    public List<View> list(Long hotelId) {
        return notifications.findTop100ByHotelIdOrderByIdDesc(hotelId).stream().map(NotificationService::view).toList();
    }

    @Transactional(readOnly = true)
    public UnreadCount unread(Long hotelId) {
        return new UnreadCount(notifications.countByHotelIdAndReadFlagFalse(hotelId));
    }

    @Transactional
    public View markRead(Long hotelId, Long id) {
        Notification notification = notifications.findByIdAndHotelId(id, hotelId)
                .orElseThrow(() -> ApiException.notFound("Notification"));
        notification.setReadFlag(true);
        return view(notification);
    }

    @Transactional
    public void markAllRead(Long hotelId) {
        for (Notification notification : notifications.findByHotelIdAndReadFlagFalse(hotelId)) {
            notification.setReadFlag(true);
        }
    }

    static View view(Notification n) {
        return new View(n.getId(), n.getTitle(), n.getMessage(), n.isReadFlag(), n.getCreatedDate());
    }
}
