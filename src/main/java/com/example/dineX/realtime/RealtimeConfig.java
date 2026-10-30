package com.example.dineX.realtime;

import com.example.dineX.security.JwtService;
import com.example.dineX.user.UserRepository;
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

    public RealtimeConfig(@Value("${app.cors.allowed-origins:http://localhost:4200,https://dinexbeta.netlify.app}") String[] allowedOrigins,
                          JwtService jwtService, UserRepository users) {
        this.allowedOrigins = allowedOrigins;
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
