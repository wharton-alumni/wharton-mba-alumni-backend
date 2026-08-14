package edu.wharton.alumni.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class VerificationEmailService {
    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailService.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String fromAddress;

    public VerificationEmailService(JavaMailSender mailSender,
                                    @Value("${app.mail.enabled}") boolean mailEnabled,
                                    @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    public void sendVerificationCode(String toEmail, String code, long ttlMinutes) {
        if (!mailEnabled) {
            logger.info("Email delivery disabled. Verification code for {} is {}", toEmail, code);
            return;
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Email sender is not configured.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Your Wharton EMBA alumni portal verification code");
        message.setText("""
                Your Wharton EMBA alumni portal verification code is:

                %s

                This code expires in %d minutes.

                This student and alumni-created website is not an official Wharton or University of Pennsylvania property.
                """.formatted(code, ttlMinutes));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            logger.warn("Unable to send verification code email to {}", toEmail, exception);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Unable to send verification code email.");
        }
    }
}
