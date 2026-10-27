package com.example.dineX.security;

import com.example.dineX.hotel.Hotel;
import com.example.dineX.hotel.HotelRepository;
import com.example.dineX.subscription.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantAccessFilter extends OncePerRequestFilter {
    private static final List<String> EXEMPT_PREFIXES = List.of("/api/v1/hotel/subscription", "/api/v1/hotel/profile");

    private final HotelRepository hotels;
    private final SubscriptionService subscriptions;

    public TenantAccessFilter(HotelRepository hotels, SubscriptionService subscriptions) {
        this.hotels = hotels;
        this.subscriptions = subscriptions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user
                && user.hotelId() != null && !user.isSuperAdmin() && isTenantPath(path) && !isExempt(path)) {
            Hotel hotel = hotels.findByIdAndDeletedFlagFalse(user.hotelId()).orElse(null);
            if (hotel == null || !hotel.isEnabled()) {
                write(response, 403, "HOTEL_DISABLED", "This hotel account is disabled");
                return;
            }
            if (!subscriptions.hasActiveSubscription(hotel.getId())) {
                write(response, 402, "SUBSCRIPTION_REQUIRED", "An active subscription is required to continue");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isTenantPath(String path) {
        return path.startsWith("/api/v1/hotel/") || path.startsWith("/api/v1/kitchen/");
    }

    private boolean isExempt(String path) {
        return EXEMPT_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
