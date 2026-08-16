package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.ForgotPasswordRequest;
import edu.wharton.alumni.dto.LoginRequest;
import edu.wharton.alumni.dto.LoginResponse;
import edu.wharton.alumni.dto.RegisterRequest;
import edu.wharton.alumni.dto.ResetPasswordRequest;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.PasswordResetToken;
import edu.wharton.alumni.model.Role;
import edu.wharton.alumni.repository.PasswordResetTokenRepository;
import edu.wharton.alumni.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {
    private final AlumniService alumniService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RateLimitService rateLimitService;
    private final EmailService emailService;
    private final long resetTokenTtlMinutes;
    private final String publicBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AlumniService alumniService, PasswordEncoder passwordEncoder, JwtService jwtService,
                       PasswordResetTokenRepository resetTokenRepository, RateLimitService rateLimitService,
                       EmailService emailService,
                       @Value("${app.auth.reset-token-ttl-minutes}") long resetTokenTtlMinutes,
                       @Value("${app.public-base-url}") String publicBaseUrl) {
        this.alumniService = alumniService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.resetTokenRepository = resetTokenRepository;
        this.rateLimitService = rateLimitService;
        this.emailService = emailService;
        this.resetTokenTtlMinutes = resetTokenTtlMinutes;
        this.publicBaseUrl = publicBaseUrl;
    }

    public LoginResponse login(LoginRequest request) {
        rateLimitService.check("login:" + normalize(request.email()), 8, 900);
        AlumniProfile profile = alumniService.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), profile.passwordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid email or password.");
        }

        return new LoginResponse(jwtService.createToken(profile), profile.withoutPassword());
    }

    public LoginResponse register(RegisterRequest request) {
        Optional<AlumniProfile> existing = alumniService.findByEmail(request.email());
        if (existing.isPresent()) {
            throw new ResponseStatusException(CONFLICT, "An alumni profile already exists for that email.");
        }

        AlumniProfile profile = new AlumniProfile(
                UUID.randomUUID(),
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.cohortCampus(),
                request.classYear(),
                request.currentTitle(),
                request.currentCompany(),
                request.industry(),
                request.city(),
                request.stateCountry(),
                request.linkedinUrl(),
                request.bio(),
                request.willingToMentor(),
                request.hiring(),
                request.avatarUrl(),
                request.bioBookProfileJson(),
                Role.ALUMNI,
                true,
                Instant.now()
        );
        AlumniProfile saved = alumniService.save(profile);
        return new LoginResponse(jwtService.createToken(saved), saved.withoutPassword());
    }

    public AlumniProfile me(UUID profileId) {
        return alumniService.findInternal(profileId).withoutPassword();
    }

    public Optional<String> forgotPassword(ForgotPasswordRequest request) {
        rateLimitService.check("forgot:" + normalize(request.email()), 5, 3600);
        Optional<AlumniProfile> profile = alumniService.findByEmail(request.email());
        if (profile.isEmpty()) {
            if (!isDemoEmail(request.email())) {
                return Optional.empty();
            }
            profile = Optional.of(alumniService.createDemoProfile(
                    normalize(request.email()),
                    passwordEncoder.encode(randomToken())
            ));
        }
        String token = randomToken();
        resetTokenRepository.save(new PasswordResetToken(
                UUID.randomUUID(),
                profile.get().id(),
                sha256(token),
                Instant.now().plusSeconds(resetTokenTtlMinutes * 60),
                null,
                Instant.now()
        ));
        emailService.sendPasswordResetLink(
                profile.get().email(),
                resetUrl(token),
                resetTokenTtlMinutes
        );
        return Optional.of(token);
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(sha256(request.token()))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid or expired reset token."));
        if (token.usedAt() != null || token.expiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid or expired reset token.");
        }
        AlumniProfile profile = alumniService.findInternal(token.profileId());
        alumniService.save(profile.withPasswordHash(passwordEncoder.encode(request.password())));
        resetTokenRepository.save(token.markUsed());
    }

    public PasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    public String tokenFor(AlumniProfile profile) {
        return jwtService.createToken(profile);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash token.", exception);
        }
    }

    private String resetUrl(String token) {
        String baseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return baseUrl + "/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private boolean isDemoEmail(String email) {
        String normalizedEmail = normalize(email);
        return normalizedEmail.split("@", 2)[0].startsWith("demo");
    }
}
