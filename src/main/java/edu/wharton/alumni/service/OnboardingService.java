package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.LoginResponse;
import edu.wharton.alumni.dto.OnboardingClaimRequest;
import edu.wharton.alumni.dto.OnboardingLookupRequest;
import edu.wharton.alumni.dto.OnboardingLookupResponse;
import edu.wharton.alumni.dto.SendCodeResponse;
import edu.wharton.alumni.dto.VerifyCodeRequest;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.BioBookProfile;
import edu.wharton.alumni.model.OnboardingCode;
import edu.wharton.alumni.repository.OnboardingCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class OnboardingService {
    private final BioBookService bioBookService;
    private final AlumniService alumniService;
    private final AuthService authService;
    private final OnboardingCodeRepository onboardingCodeRepository;
    private final RateLimitService rateLimitService;
    private final EmailService emailService;
    private final long verificationCodeTtlMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public OnboardingService(BioBookService bioBookService, AlumniService alumniService, AuthService authService,
                             OnboardingCodeRepository onboardingCodeRepository, RateLimitService rateLimitService,
                             EmailService emailService,
                             @Value("${app.auth.verification-code-ttl-minutes}") long verificationCodeTtlMinutes) {
        this.bioBookService = bioBookService;
        this.alumniService = alumniService;
        this.authService = authService;
        this.onboardingCodeRepository = onboardingCodeRepository;
        this.rateLimitService = rateLimitService;
        this.emailService = emailService;
        this.verificationCodeTtlMinutes = verificationCodeTtlMinutes;
    }

    public OnboardingLookupResponse lookup(OnboardingLookupRequest request) {
        Optional<AlumniProfile> existingProfile = alumniService.findByEmail(request.email());
        if (existingProfile.isPresent()) {
            AlumniProfile alumniProfile = existingProfile.get();
            return new OnboardingLookupResponse(
                    true,
                    true,
                    alumniProfile.firstName() + " " + alumniProfile.lastName(),
                    alumniProfile.cohortCampus().label(),
                    "WEMBA'" + (alumniProfile.classYear() - 1976),
                    alumniProfile.currentCompany(),
                    alumniProfile.currentTitle()
            );
        }
        Optional<BioBookProfile> profile = bioBookService.findBioBookProfile(request.email());
        if (profile.isEmpty()) {
            if (isDemoEmail(request.email())) {
                String localPart = normalize(request.email()).split("@", 2)[0];
                return new OnboardingLookupResponse(
                        true,
                        false,
                        titleCase(localPart.replace('.', ' ').replace('_', ' ').replace('-', ' ')),
                        "Philadelphia",
                        "WEMBA'52",
                        "Wharton Alumni Portal",
                        "Demo Alumni"
                );
            }
            return new OnboardingLookupResponse(false, false, null, null, null, null, null);
        }
        BioBookProfile bioBookProfile = profile.get();
        return new OnboardingLookupResponse(
                true,
                false,
                bioBookProfile.fullLegalName(),
                bioBookProfile.cohort(),
                bioBookProfile.batch(),
                bioBookProfile.currentEmployer(),
                bioBookProfile.currentTitleRole()
        );
    }

    public SendCodeResponse sendCode(OnboardingLookupRequest request) {
        String email = normalize(request.email());
        rateLimitService.check("onboarding-code:" + email, 5, 900);

        String code = verificationCode();
        onboardingCodeRepository.save(new OnboardingCode(
                UUID.randomUUID(),
                email,
                sha256(code),
                Instant.now().plusSeconds(verificationCodeTtlMinutes * 60),
                null,
                Instant.now()
        ));
        emailService.sendVerificationCode(email, code, verificationCodeTtlMinutes);
        return new SendCodeResponse("Verification code sent.");
    }

    public SendCodeResponse verifyCode(VerifyCodeRequest request) {
        verify(request.email(), request.code());
        return new SendCodeResponse("Verification code verified.");
    }

    public LoginResponse claim(OnboardingClaimRequest request) {
        String email = normalize(request.email());
        if (alumniService.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "An alumni profile already exists for that email.");
        }
        Optional<BioBookProfile> bioBookProfile = bioBookService.findBioBookProfile(email);
        if (bioBookProfile.isEmpty() && isDemoEmail(email)) {
            AlumniProfile profile = alumniService.createDemoProfile(email, authService.passwordEncoder().encode(request.password()));
            return new LoginResponse(authService.tokenFor(profile), profile.withoutPassword());
        }
        BioBookProfile profileData = bioBookProfile
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No BioBook profile found for that work email."));
        AlumniProfile profile = bioBookService.createClaimedAlumniProfile(email, request.password(), profileData);
        return new LoginResponse(authService.tokenFor(profile), profile.withoutPassword());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isDemoEmail(String email) {
        String normalizedEmail = normalize(email);
        return normalizedEmail.split("@", 2)[0].startsWith("demo");
    }

    private void verify(String email, String code) {
        String normalizedEmail = normalize(email);
        rateLimitService.check("verify-code:" + normalizedEmail, 8, 900);
        OnboardingCode onboardingCode = onboardingCodeRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid or expired verification code."));
        if (onboardingCode.verifiedAt() != null || onboardingCode.expiresAt().isBefore(Instant.now())
                || !onboardingCode.codeHash().equals(sha256(code.trim()))) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid or expired verification code.");
        }
        onboardingCodeRepository.save(onboardingCode.markVerified());
    }

    private String verificationCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash verification code.", exception);
        }
    }

    private String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "Demo User";
        }
        String[] words = value.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.isEmpty() ? "Demo User" : result.toString();
    }
}
