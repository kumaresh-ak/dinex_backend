package com.example.dineX.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RealtimePublisher {
    public record Event(String type, Object payload) { }

    private final SimpMessagingTemplate messaging;

    public RealtimePublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    /**
     * Publishes to the hotel-wide events topic and, when present, the guest session topic.
     *
     * <p>Inside a transaction the message is sent only after the transaction commits. Otherwise a
     * client that reloads on the event can read the database before the change is visible and show
     * stale data until the next manual refresh.
     */
    public void publish(Long hotelId, String sessionToken, String type, Object payload) {
        Event event = new Event(type, payload);
        Runnable send = () -> {
            messaging.convertAndSend("/topic/hotels/" + hotelId + "/events", event);
            if (sessionToken != null && !sessionToken.isBlank()) {
                messaging.convertAndSend("/topic/sessions/" + sessionToken, event);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
