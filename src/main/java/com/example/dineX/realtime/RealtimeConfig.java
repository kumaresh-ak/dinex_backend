package com.example.dineX.realtime;

import com.example.dineX.security.JwtService;
import com.example.dineX.user.UserRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class RealtimeConfig implements WebSocketMessageBrokerConfigurer {
    private final String[] allowedOrigins;
    private final JwtService jwtService;
    private final UserRepository users;

<<<<<<< HEAD
    public RealtimeConfig(@Value("${app.cors.allowed-origins:http://localhost:4200,https://dinexbeta.netlify.app}") String[] allowedOrigins,
=======
    public RealtimeConfig(@Value("${app.cors.allowed-origins:http://localhost:4200}") String configuredOrigins,
>>>>>>> afefc8534ab110fd5f642521f2002216ccd9892d
                          JwtService jwtService, UserRepository users) {
        // Split and trim explicitly: an untrimmed entry (for example " https://...") never matches the Origin header.
        Set<String> origins = new LinkedHashSet<>();
        for (String origin : (configuredOrigins + ",https://dinexbeta.netlify.app").split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                origins.add(trimmed);
            }
        }
        this.allowedOrigins = origins.toArray(String[]::new);
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthChannelInterceptor(jwtService, users));
    }
}
