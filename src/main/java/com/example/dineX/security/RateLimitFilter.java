package com.example.dineX.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_REQUESTS_PER_MINUTE_AUTH = 10;
    private static final int MAX_REQUESTS_PER_MINUTE_PUBLIC = 60;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int limit = limitFor(request.getRequestURI());
        if (limit == 0) {
            chain.doFilter(request, response);
            return;
        }
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.clear();
        }
        String key = request.getRemoteAddr() + "|" + (limit == MAX_REQUESTS_PER_MINUTE_AUTH ? "auth" : "public");
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) ->
                existing == null || now - existing.startedAt >= WINDOW_MILLIS ? new Window(now) : existing);
        if (window.count.incrementAndGet() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests, try again later\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private int limitFor(String path) {
        if (path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/forgot-password")
                || path.startsWith("/api/v1/auth/reset-password")) {
            return MAX_REQUESTS_PER_MINUTE_AUTH;
        }
        if (path.startsWith("/api/v1/public/files/")) {
            return 0;
        }
        if (path.startsWith("/api/v1/public/")) {
            return MAX_REQUESTS_PER_MINUTE_PUBLIC;
        }
        return 0;
    }

    private static final class Window {
        private final long startedAt;
        private final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
