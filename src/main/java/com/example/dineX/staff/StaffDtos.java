package com.example.dineX.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class StaffDtos {
    private StaffDtos() { }

    public record StaffView(Long id, String displayName, String username, String mobileNumber, boolean enabled,
                            LocalDateTime lastLoginAt) { }

    public record CreateStaffRequest(
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Email @Size(max = 190) String username,
            @Pattern(regexp = "^[0-9]{10}$", message = "must be a 10 digit mobile number") String mobileNumber,
            @NotBlank @Size(min = 8, max = 72) String password) { }

    public record StatusRequest(@NotNull Boolean enabled) { }
}
