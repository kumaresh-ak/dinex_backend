package com.example.dineX.settings;

import com.example.dineX.settings.PlatformSettingsService.SettingView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformSettingsController {

    public record UpdateSettingRequest(@NotNull @Size(max = 500) String value) { }

    private final PlatformSettingsService settings;

    public PlatformSettingsController(PlatformSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public List<SettingView> list() {
        return settings.list();
    }

    @PutMapping("/{key}")
    public SettingView update(@PathVariable String key, @Valid @RequestBody UpdateSettingRequest request) {
        return settings.update(key, request.value());
    }
}
