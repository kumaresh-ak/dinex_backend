package com.example.dineX.user;

import com.example.dineX.security.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SuperAdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;

    public SuperAdminBootstrap(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.super-admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.super-admin.username:}") String username,
            @Value("${app.bootstrap.super-admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Super-admin bootstrap is enabled, but its username or password is not configured");
        }

        User existing = users.findByUsernameAndDeletedFlagFalse(username).orElse(null);
        if (existing != null) {
            if (existing.getRole() != UserRole.SUPER_ADMIN) {
                throw new IllegalStateException(
                        "The configured super-admin username already belongs to a non-super-admin account");
            }
            log.info("Super-admin bootstrap skipped because the account already exists");
            return;
        }

        if (users.existsByUsername(username)) {
            throw new IllegalStateException(
                    "The configured super-admin username belongs to a deleted account and cannot be reused");
        }

        users.save(new User(null, username, passwordEncoder.encode(password),
                "DineX Platform Administrator", null, UserRole.SUPER_ADMIN));
        log.info("Bootstrapped the configured super-admin account");
    }
}
