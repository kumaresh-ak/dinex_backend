package com.example.dineX.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Short-lived hotel status cache for tenant requests; evicted after commit when hotel or subscription state changes. */
@Component
public class TenantStatusCache {
    private static final long TTL_MILLIS = 30_000L;

    public record Status(boolean enabled, boolean subscribed) { }

    private record Entry(Status status, long expiresAt) { }

    private final Map<Long, Entry> entries = new ConcurrentHashMap<>();

    public Status get(Long hotelId, Function<Long, Status> loader) {
        long now = System.currentTimeMillis();
        Entry entry = entries.get(hotelId);
        if (entry != null && entry.expiresAt() > now) {
            return entry.status();
        }
        Status status = loader.apply(hotelId);
        entries.put(hotelId, new Entry(status, now + TTL_MILLIS));
        return status;
    }

    public void evict(Long hotelId) {
        afterCommit(() -> entries.remove(hotelId));
    }

    public void evictAll() {
        afterCommit(entries::clear);
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
