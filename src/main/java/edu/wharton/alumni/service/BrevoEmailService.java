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
import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService implements EmailService {
    private final ObjectMapper objectMapper;
    private final String configuredApiKey;
    private final String apiKeyFile;
    private final String senderName;
    private final String senderEmail;
    private final HttpClient httpClient;
    private String apiKey;

    public BrevoEmailService(
            ObjectMapper objectMapper,
            @Value("${app.email.brevo.api-key:}") String configuredApiKey,
            @Value("${app.email.brevo.api-key-file:}") String apiKeyFile,
            @Value("${app.email.sender.name}") String senderName,
            @Value("${app.email.sender.email}") String senderEmail
    ) {
        this.objectMapper = objectMapper;
        this.configuredApiKey = configuredApiKey;
        this.apiKeyFile = apiKeyFile;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
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
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email service is not configured.");
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", "Your Wharton EMBA verification code",
                    "htmlContent", "<p>Your verification code is: <strong>" + code + "</strong>. It expires in " + ttlMinutes + " minutes.</p>"
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
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to send verification email.");
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to send verification email.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to send verification email.", exception);
        }
    }
}
