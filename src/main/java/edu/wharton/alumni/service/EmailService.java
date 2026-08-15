package edu.wharton.alumni.service;

public interface EmailService {
    void sendVerificationCode(String toEmail, String code, long ttlMinutes);

    void sendPasswordResetLink(String toEmail, String resetUrl, long ttlMinutes);
}
