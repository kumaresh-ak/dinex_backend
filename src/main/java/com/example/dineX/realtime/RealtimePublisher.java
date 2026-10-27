package com.example.dineX.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RealtimePublisher {
    public record Event(String type, Object payload) { }

    private final SimpMessagingTemplate messaging;

    public RealtimePublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    /** Publishes to the hotel-wide events topic and, when present, the guest session topic. */
    public void publish(Long hotelId, String sessionToken, String type, Object payload) {
        Event event = new Event(type, payload);
        messaging.convertAndSend("/topic/hotels/" + hotelId + "/events", event);
        if (sessionToken != null && !sessionToken.isBlank()) {
            messaging.convertAndSend("/topic/sessions/" + sessionToken, event);
        }
    }
}
