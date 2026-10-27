package com.example.dineX.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration accessTokenLifetime;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-minutes:15}") long accessTokenMinutes) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret must contain at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenLifetime = Duration.ofMinutes(accessTokenMinutes);
    }

    public String issueAccessToken(String username, Long hotelId, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claims(Map.of("hotelId", hotelId == null ? 0L : hotelId, "role", role.name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenLifetime)))
                .signWith(key)
                .compact();
    }

    public SecretKey signingKey() { return key; }

    public long accessTokenSeconds() { return accessTokenLifetime.toSeconds(); }

    public String subject(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
