package com.example.dineX.auth;

import com.example.dineX.config.AppProperties;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final Optional<JavaMailSender> mailSender;
    private final AppProperties properties;

    public EmailService(ObjectProvider<JavaMailSender> mailSender, AppProperties properties) {
        this.mailSender = Optional.ofNullable(mailSender.getIfAvailable());
        this.properties = properties;
    }

    public void sendPasswordReset(String to, String rawToken) {
        String link = properties.publicBaseUrl() + "/reset-password?token=" + rawToken;
        String body = "A password reset was requested for your Systrns DineX account.\n\n"
                + "Open the link below to choose a new password. It expires in 30 minutes.\n\n"
                + link + "\n\nIf you did not request this, ignore this email.";
        if (mailSender.isEmpty()) {
            log.warn("Mail sender is not configured; password reset email not sent to {}", to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mail().from());
        message.setTo(to);
        message.setSubject("Reset your Systrns DineX password");
        message.setText(body);
        mailSender.get().send(message);
    }
}
