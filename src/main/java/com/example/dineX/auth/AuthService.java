package com.example.dineX.auth;

import com.example.dineX.audit.AuditService;
import com.example.dineX.common.ApiException;
import com.example.dineX.security.JwtService;
import com.example.dineX.user.User;
import com.example.dineX.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RESET_MINUTES = 30;

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService email;
    private final AuditService audit;
    private final long refreshDays;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordResetTokenRepository resetTokens, PasswordEncoder passwordEncoder,
                       JwtService jwtService, EmailService email, AuditService audit,
                       @Value("${app.jwt.refresh-token-days:7}") long refreshDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.email = email;
        this.audit = audit;
        this.refreshDays = refreshDays;
    }

    public record Session(String accessToken, String refreshToken, long expiresInSeconds, User user) { }

    @Transactional
    public Session login(String username, String password) {
        User user = users.findByUsernameAndEnabledTrueAndDeletedFlagFalse(username)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password"));
        user.recordLogin();
        users.save(user);
        audit.record(user.getHotelId(), "LOGIN", "User", user.getId(), null);
        return issue(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Session refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw invalidRefresh();
        }
        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawRefreshToken)).orElseThrow(this::invalidRefresh);
        if (stored.isRevoked()) {
            // Reuse of a rotated token: treat as theft and revoke every session of this user.
            refreshTokens.revokeAllForUser(stored.getUserId());
            throw invalidRefresh();
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw invalidRefresh();
        }
        stored.revoke();
        refreshTokens.save(stored);
        User user = users.findByIdAndDeletedFlagFalse(stored.getUserId())
                .filter(User::isEnabled)
                .orElseThrow(this::invalidRefresh);
        return issue(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            token.revoke();
            refreshTokens.save(token);
        });
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = users.findByIdAndDeletedFlagFalse(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("New password must differ from the current password");
        }
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        refreshTokens.revokeAllForUser(userId);
        audit.record(user.getHotelId(), "PASSWORD_CHANGED", "User", userId, null);
    }

    /** Always completes silently so that account existence is never revealed. */
    @Transactional
    public void forgotPassword(String username) {
        users.findByUsernameAndEnabledTrueAndDeletedFlagFalse(username).ifPresent(user -> {
            if (!username.contains("@")) {
                return;
            }
            String raw = randomToken();
            resetTokens.save(new PasswordResetToken(user.getId(), hash(raw), LocalDateTime.now().plusMinutes(RESET_MINUTES)));
            email.sendPasswordReset(username, raw);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> ApiException.badRequest("Reset link is invalid or has expired"));
        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Reset link is invalid or has expired");
        }
        User user = users.findByIdAndDeletedFlagFalse(token.getUserId())
                .orElseThrow(() -> ApiException.badRequest("Reset link is invalid or has expired"));
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        token.markUsed();
        resetTokens.save(token);
        refreshTokens.revokeAllForUser(user.getId());
        audit.record(user.getHotelId(), "PASSWORD_RESET", "User", user.getId(), null);
    }

    @Transactional(readOnly = true)
    public User currentUser(Long userId) {
        return users.findByIdAndDeletedFlagFalse(userId).orElseThrow(() -> ApiException.notFound("User"));
    }

    private Session issue(User user) {
        String access = jwtService.issueAccessToken(user.getUsername(), user.getHotelId(), user.getRole());
        String raw = randomToken();
        refreshTokens.save(new RefreshToken(user.getId(), hash(raw), LocalDateTime.now().plusDays(refreshDays)));
        return new Session(access, raw, jwtService.accessTokenSeconds(), user);
    }

    private ApiException invalidRefresh() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session expired. Please sign in again");
    }

    static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
