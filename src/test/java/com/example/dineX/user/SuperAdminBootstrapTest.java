package com.example.dineX.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.dineX.security.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SuperAdminBootstrapTest {
    private static final String USERNAME = "6381110664";
    private static final String PASSWORD = "TestPassword123!";

    private final UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createsSuperAdminWithEncodedPasswordWhenEnabled() {
        when(users.findByUsernameAndDeletedFlagFalse(USERNAME)).thenReturn(Optional.empty());
        when(users.existsByUsername(USERNAME)).thenReturn(false);

        bootstrap(true, USERNAME, PASSWORD).run(new DefaultApplicationArguments());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User created = captor.getValue();
        assertEquals(USERNAME, created.getUsername());
        assertEquals(UserRole.SUPER_ADMIN, created.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(PASSWORD, created.getPasswordHash()));
    }

    @Test
    void doesNotResetExistingSuperAdminPassword() {
        User existing = new User(null, USERNAME, "existing-hash", "Existing Admin", null, UserRole.SUPER_ADMIN);
        when(users.findByUsernameAndDeletedFlagFalse(USERNAME)).thenReturn(Optional.of(existing));

        bootstrap(true, USERNAME, PASSWORD).run(new DefaultApplicationArguments());

        verify(users, never()).save(any());
    }

    @Test
    void refusesToReuseUsernameOwnedByAnotherRole() {
        User existing = new User(null, USERNAME, "existing-hash", "Hotel Admin", null, UserRole.HOTEL_ADMIN);
        when(users.findByUsernameAndDeletedFlagFalse(USERNAME)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> bootstrap(true, USERNAME, PASSWORD).run(new DefaultApplicationArguments()));

        verify(users, never()).save(any());
    }

    @Test
    void requiresPasswordWhenBootstrapIsEnabled() {
        assertThrows(IllegalStateException.class,
                () -> bootstrap(true, USERNAME, "").run(new DefaultApplicationArguments()));
        verify(users, never()).save(any());
    }

    private SuperAdminBootstrap bootstrap(boolean enabled, String username, String password) {
        return new SuperAdminBootstrap(users, passwordEncoder, enabled, username, password);
    }
}
