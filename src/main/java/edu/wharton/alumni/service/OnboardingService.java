package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.LoginResponse;
import edu.wharton.alumni.dto.MessageResponse;
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
    private final OnboardingCodeRepository codeRepository;
    private final RateLimitService rateLimitService;
    private final long codeTtlMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public OnboardingService(BioBookService bioBookService, AlumniService alumniService, AuthService authService,
                             OnboardingCodeRepository codeRepository, RateLimitService rateLimitService,
                             @Value("${app.auth.verification-code-ttl-minutes}") long codeTtlMinutes) {
        this.bioBookService = bioBookService;
        this.alumniService = alumniService;
        this.authService = authService;
        this.codeRepository = codeRepository;
        this.rateLimitService = rateLimitService;
        this.codeTtlMinutes = codeTtlMinutes;
    }

    public OnboardingLookupResponse lookup(OnboardingLookupRequest request) {
        Optional<BioBookProfile> profile = bioBookService.findBioBookProfile(request.email());
        if (profile.isEmpty()) {
            return new OnboardingLookupResponse(false, false, null, null, null, null, null);
        }
        BioBookProfile bioBookProfile = profile.get();
        return new OnboardingLookupResponse(
                true,
                alumniService.findByEmail(request.email()).isPresent(),
                bioBookProfile.fullLegalName(),
                bioBookProfile.cohort(),
                bioBookProfile.batch(),
                bioBookProfile.currentEmployer(),
                bioBookProfile.currentTitleRole()
        );
    }

    public SendCodeResponse sendCode(OnboardingLookupRequest request) {
        String email = normalize(request.email());
        rateLimitService.check("code:" + email, 5, 900);
        bioBookService.findBioBookProfile(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No BioBook profile found for that work email."));
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        codeRepository.save(new OnboardingCode(
                UUID.randomUUID(),
                email,
                sha256(code),
                Instant.now().plusSeconds(codeTtlMinutes * 60),
                null,
                Instant.now()
        ));
        return new SendCodeResponse("Verification code sent.", code);
    }

    public MessageResponse verifyCode(VerifyCodeRequest request) {
        verify(request.email(), request.code());
        return new MessageResponse("Verification complete.");
    }

    public LoginResponse claim(OnboardingClaimRequest request) {
        String email = normalize(request.email());
        BioBookProfile bioBookProfile = bioBookService.findBioBookProfile(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No BioBook profile found for that work email."));
        if (alumniService.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "An alumni profile already exists for that email.");
        }
        verify(email, request.code());
        AlumniProfile profile = bioBookService.createClaimedAlumniProfile(email, request.password(), bioBookProfile);
        return new LoginResponse(authService.tokenFor(profile), profile.withoutPassword());
    }

    private void verify(String email, String code) {
        String normalizedEmail = normalize(email);
        OnboardingCode onboardingCode = codeRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid or expired verification code."));
        if (onboardingCode.expiresAt().isBefore(Instant.now()) || !onboardingCode.codeHash().equals(sha256(code))) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid or expired verification code.");
        }
        if (onboardingCode.verifiedAt() == null) {
            codeRepository.save(onboardingCode.markVerified());
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash verification code.", exception);
        }
    }
}
