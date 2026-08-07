package edu.wharton.alumni.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wharton.alumni.dto.BioBookClaimResponse;
import edu.wharton.alumni.dto.BioBookLookupResponse;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.BioBookProfile;
import edu.wharton.alumni.model.Role;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class BioBookService {
    private final AlumniService alumniService;
    private final AuthService authService;
    private final List<BioBookClaimRecord> claimRecords;

    public BioBookService(AlumniService alumniService, AuthService authService, ObjectMapper objectMapper) throws IOException {
        this.alumniService = alumniService;
        this.authService = authService;
        this.claimRecords = objectMapper.readValue(
                new ClassPathResource("seed/biobook-claims.json").getInputStream(),
                new TypeReference<>() {
                }
        );
    }

    public BioBookLookupResponse lookup(String email) {
        Optional<BioBookProfile> profile = findProfileByEmail(email);
        return new BioBookLookupResponse(profile.isPresent(), profile.orElse(null));
    }

    public BioBookClaimResponse claim(String email, String password) {
        BioBookProfile bioBookProfile = findProfileByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No BioBook profile found for that email."));

        if (alumniService.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "An alumni profile already exists for that email. Please log in with your password.");
        }

        AlumniProfile profile = toAlumniProfile(email, password, bioBookProfile);
        AlumniProfile saved = alumniService.save(profile).withoutPassword();
        return new BioBookClaimResponse("demo-token-" + saved.id(), saved, bioBookProfile);
    }

    private Optional<BioBookProfile> findProfileByEmail(String email) {
        String hash = sha256(email.trim().toLowerCase(Locale.ROOT));
        return claimRecords.stream()
                .filter(record -> record.emailHashes().contains(hash))
                .map(BioBookClaimRecord::profile)
                .findFirst();
    }

    private AlumniProfile toAlumniProfile(String email, String password, BioBookProfile bioBookProfile) {
        String fullName = valueOr(bioBookProfile.fullLegalName(), "Wharton Alumni");
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts.length > 0 ? parts[0] : fullName;
        String lastName = parts.length > 1 ? parts[1] : firstName;
        String bio = firstNonBlank(
                bioBookProfile.careerTrajectoryIn3Bullets(),
                bioBookProfile.canHelpClassmatesWith(),
                "Wharton Executive MBA alumni profile."
        );

        return new AlumniProfile(
                UUID.randomUUID(),
                email.trim().toLowerCase(Locale.ROOT),
                authService.passwordEncoder().encode(password),
                firstName,
                lastName,
                "Not provided",
                bioBookProfile.cohortCampus(),
                bioBookProfile.classYear(),
                valueOr(bioBookProfile.currentTitleRole(), "Wharton Executive MBA Alumni"),
                valueOr(bioBookProfile.currentEmployer(), "Not provided"),
                valueOr(bioBookProfile.industry(), "Not provided"),
                valueOr(bioBookProfile.city(), ""),
                valueOr(bioBookProfile.stateCountry(), ""),
                valueOr(bioBookProfile.linkedinUrl(), ""),
                bio,
                bioBookProfile.willingToMentor(),
                false,
                null,
                Role.ALUMNI,
                true,
                Instant.now()
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
