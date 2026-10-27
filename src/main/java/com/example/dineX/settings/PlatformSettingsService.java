package com.example.dineX.settings;

import com.example.dineX.audit.AuditService;
import com.example.dineX.common.ApiException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Whitelisted platform-wide settings stored in the platform_settings table. */
@Service
public class PlatformSettingsService {

    public enum Key {
        SUPPORT_EMAIL("support.email", "Support email shown to hotel owners"),
        SUPPORT_PHONE("support.phone", "Support phone shown to hotel owners"),
        INVOICE_FOOTER("invoice.footer", "Footer text printed on bills"),
        PAYMENT_UPI_ID("payment.upi_id", "UPI ID hotel owners pay subscription fees to");

        private final String code;
        private final String description;

        Key(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String code() {
            return code;
        }

        public String description() {
            return description;
        }

        public static Key fromCode(String code) {
            for (Key key : values()) {
                if (key.code.equals(code)) {
                    return key;
                }
            }
            throw ApiException.notFound("Unknown setting: " + code);
        }
    }

    public record SettingView(String key, String description, String value) { }

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public PlatformSettingsService(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<SettingView> list() {
        Map<String, String> stored = stored();
        return Arrays.stream(Key.values())
                .map(k -> new SettingView(k.code(), k.description(), stored.getOrDefault(k.code(), "")))
                .toList();
    }

    /** Returns the stored value, or an empty string when the setting has never been saved. */
    @Transactional(readOnly = true)
    public String value(Key key) {
        return stored().getOrDefault(key.code(), "");
    }

    @Transactional
    public SettingView update(String code, String rawValue) {
        Key key = Key.fromCode(code);
        String value = rawValue == null ? "" : rawValue.trim();
        if (key == Key.SUPPORT_EMAIL && !value.isEmpty() && !EMAIL.matcher(value).matches()) {
            throw ApiException.badRequest("Support email is not a valid address");
        }
        jdbc.update("INSERT INTO platform_settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)", key.code(), value);
        audit.record(null, "SETTING_UPDATED", "PLATFORM_SETTING", key.code(), "updated " + key.code());
        return new SettingView(key.code(), key.description(), value);
    }

    private Map<String, String> stored() {
        Map<String, String> map = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT setting_key, setting_value FROM platform_settings")) {
            map.put((String) row.get("setting_key"), (String) row.get("setting_value"));
        }
        return map;
    }
}
