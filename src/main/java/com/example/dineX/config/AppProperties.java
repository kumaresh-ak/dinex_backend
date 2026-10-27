package com.example.dineX.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String publicBaseUrl, boolean refreshCookieSecure, String storageDir, Tax tax, Mail mail) {

    public record Tax(BigDecimal sgstPercent, BigDecimal cgstPercent) { }

    public record Mail(String from) { }
}
