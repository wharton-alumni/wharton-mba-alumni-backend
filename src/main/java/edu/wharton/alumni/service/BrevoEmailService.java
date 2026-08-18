package edu.wharton.alumni.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BrevoEmailService implements EmailService {
    private final ObjectMapper objectMapper;
    private final String configuredApiKey;
    private final String apiKeyFile;
    private final String senderName;
    private final String senderEmail;
    private final List<String> demoRecipients;
    private final HttpClient httpClient;
    private String apiKey;

    public BrevoEmailService(
            ObjectMapper objectMapper,
            @Value("${app.email.brevo.api-key:}") String configuredApiKey,
            @Value("${app.email.brevo.api-key-file:}") String apiKeyFile,
            @Value("${app.email.sender.name}") String senderName,
            @Value("${app.email.sender.email}") String senderEmail,
            @Value("${app.email.demo-recipients:}") String demoRecipients
    ) {
        this.objectMapper = objectMapper;
        this.configuredApiKey = configuredApiKey;
        this.apiKeyFile = apiKeyFile;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.demoRecipients = parseRecipients(demoRecipients);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostConstruct
    public void loadApiKey() {
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            apiKey = configuredApiKey.trim();
            return;
        }
        if (apiKeyFile == null || apiKeyFile.isBlank()) {
            return;
        }
        FileSystemResource resource = new FileSystemResource(apiKeyFile);
        if (!resource.exists()) {
            return;
        }
        try {
            apiKey = Files.readString(resource.getFile().toPath()).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read Brevo API key file.", exception);
        }
    }

    @Override
    public void sendVerificationCode(String toEmail, String code, long ttlMinutes) {
        sendEmail(
                toEmail,
                "Your Wharton Alumni Portal verification code",
                """
                        <p><strong>Verification Code: %s</strong></p>
                        <p>Hello,</p>
                        <p>Use the code above to complete your verification for the Wharton Alumni Portal. This code is valid for %d minutes.</p>
                        <p>If you did not request this code, you can safely ignore this email.</p>
                        <p>Best regards,</p>
                        <p>Wharton Alumni Portal Team</p>
                        """.formatted(escapeHtml(code), ttlMinutes)
        );
    }

    @Override
    public void sendPasswordResetLink(String toEmail, String resetUrl, long ttlMinutes) {
        String safeResetUrl = escapeHtml(resetUrl);
        sendEmail(
                toEmail,
                "Your Wharton Alumni Portal password reset link",
                """
                        <p>We received a request to reset your Wharton Alumni Portal password.</p>
                        <p><a href="%s">Reset your password</a></p>
                        <p>This link expires in %d minutes. If you did not request this, you can ignore this email.</p>
                        """.formatted(safeResetUrl, ttlMinutes)
        );
    }

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        List<String> recipients = routeRecipients(toEmail);
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("=== LOCAL DEV: Email service not configured — printing email instead ===");
            System.out.println("To: " + recipients);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + htmlContent.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim());
            System.out.println("=== END EMAIL ===");
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", recipients.stream().map(recipient -> Map.of("email", recipient)).toList(),
                    "subject", subject,
                    "htmlContent", htmlContent
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(20))
                    .header("accept", "application/json")
                    .header("api-key", apiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to send email.");
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to send email.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to send email.", exception);
        }
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private List<String> routeRecipients(String toEmail) {
        if (toEmail == null) {
            return List.of();
        }
        String normalized = toEmail.trim().toLowerCase();
        String localPart = normalized.split("@", 2)[0];
        if (!localPart.startsWith("demo")) {
            return List.of(normalized);
        }
        if (demoRecipients.isEmpty()) {
            return List.of(normalized);
        }
        Set<String> recipients = new LinkedHashSet<>(demoRecipients);
        return new ArrayList<>(recipients);
    }

    private List<String> parseRecipients(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(recipient -> !recipient.isBlank())
                .distinct()
                .toList();
    }
}
