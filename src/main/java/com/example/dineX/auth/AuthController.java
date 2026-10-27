package com.example.dineX.auth;

import com.example.dineX.config.AppProperties;
import com.example.dineX.security.AuthUser;
import com.example.dineX.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    static final String REFRESH_COOKIE = "dinex_refresh";
    private static final String PASSWORD_POLICY = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    private static final String PASSWORD_POLICY_MESSAGE =
            "Password must be 8-72 characters and contain at least one letter and one number";

    private final AuthService auth;
    private final AppProperties properties;
    private final long refreshDays;

    public AuthController(AuthService auth, AppProperties properties,
                          @Value("${app.jwt.refresh-token-days:7}") long refreshDays) {
        this.auth = auth;
        this.properties = properties;
        this.refreshDays = refreshDays;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.Session session = auth.login(request.username().trim(), request.password());
        return withSession(session);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return withSession(auth.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        auth.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        auth.changePassword(user.id(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthUser user) {
        User account = auth.currentUser(user.id());
        return new MeResponse(account.getUsername(), account.getDisplayName(), account.getRole().name(),
                account.getHotelId(), account.getLastLoginAt());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        auth.forgotPassword(request.username().trim());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        auth.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<LoginResponse> withSession(AuthService.Session session) {
        User user = session.user();
        LoginResponse body = new LoginResponse(session.accessToken(), session.expiresInSeconds(), user.getUsername(),
                user.getDisplayName(), user.getRole().name(), user.getHotelId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(session.refreshToken(),
                        Duration.ofDays(refreshDays)).toString())
                .body(body);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }

    public record LoginRequest(@NotBlank @Size(max = 190) String username, @NotBlank @Size(max = 72) String password) { }

    public record LoginResponse(String accessToken, long expiresIn, String username, String displayName, String role,
                                Long hotelId) { }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Pattern(regexp = PASSWORD_POLICY, message = PASSWORD_POLICY_MESSAGE)
                                        String newPassword) { }

    public record ForgotPasswordRequest(@NotBlank @Size(max = 190) String username) { }

    public record ResetPasswordRequest(@NotBlank String token,
                                       @NotBlank @Pattern(regexp = PASSWORD_POLICY, message = PASSWORD_POLICY_MESSAGE)
                                       String newPassword) { }

    public record MeResponse(String username, String displayName, String role, Long hotelId,
                             java.time.LocalDateTime lastLoginAt) { }
}
