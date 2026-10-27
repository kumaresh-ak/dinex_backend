package com.example.dineX.notification;

import com.example.dineX.security.AuthUser;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotel/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationService.View> list(@AuthenticationPrincipal AuthUser user) {
        return notifications.list(user.hotelId());
    }

    @GetMapping("/unread-count")
    public NotificationService.UnreadCount unread(@AuthenticationPrincipal AuthUser user) {
        return notifications.unread(user.hotelId());
    }

    @PatchMapping("/{id}/read")
    public NotificationService.View markRead(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return notifications.markRead(user.hotelId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AuthUser user) {
        notifications.markAllRead(user.hotelId());
    }
}
