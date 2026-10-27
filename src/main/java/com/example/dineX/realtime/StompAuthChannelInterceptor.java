package com.example.dineX.realtime;

import com.example.dineX.security.AuthUser;
import com.example.dineX.security.JwtService;
import com.example.dineX.user.UserRepository;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authenticates STOMP CONNECT frames with the same JWT used for REST calls and
 * restricts hotel event topics to users of that hotel (or super admins).
 */
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private static final Pattern HOTEL_EVENTS = Pattern.compile("^/topic/hotels/(\\d+)/events$");
    private static final Pattern SESSION_EVENTS = Pattern.compile("^/topic/sessions/[A-Za-z0-9]{32,64}$");

    private final JwtService jwtService;
    private final UserRepository users;

    public StompAuthChannelInterceptor(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Guests connect anonymously; their session topic is protected by the unguessable session token.
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null) {
                AuthUser user = authenticate(header);
                accessor.setUser(new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))));
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && SESSION_EVENTS.matcher(destination).matches()) {
                return message;
            }
            authorizeSubscription(accessor.getUser(), destination);
        }
        return message;
    }

    private AuthUser authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("Authentication required");
        }
        try {
            String username = jwtService.subject(header.substring(7));
            return users.findByUsernameAndEnabledTrueAndDeletedFlagFalse(username)
                    .map(u -> new AuthUser(u.getId(), u.getUsername(), u.getHotelId(), u.getRole()))
                    .orElseThrow(() -> new AccessDeniedException("Authentication required"));
        } catch (RuntimeException invalid) {
            throw new AccessDeniedException("Invalid or expired token");
        }
    }

    private void authorizeSubscription(java.security.Principal principal, String destination) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new AccessDeniedException("Authentication required");
        }
        if (destination == null) {
            throw new AccessDeniedException("Destination required");
        }
        Matcher hotel = HOTEL_EVENTS.matcher(destination);
        if (hotel.matches()) {
            long hotelId = Long.parseLong(hotel.group(1));
            if (user.isSuperAdmin() || hotelId == user.hotelId()) {
                return;
            }
            throw new AccessDeniedException("Not allowed to subscribe to this hotel");
        }
        if (SESSION_EVENTS.matcher(destination).matches()) {
            return;
        }
        throw new AccessDeniedException("Unknown destination");
    }
}
