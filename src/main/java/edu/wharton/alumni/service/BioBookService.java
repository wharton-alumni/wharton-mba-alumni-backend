package edu.wharton.alumni.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wharton.alumni.dto.BioBookClaimResponse;
import edu.wharton.alumni.dto.BioBookLookupResponse;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.BioBookClaim;
import edu.wharton.alumni.model.BioBookClaimEmail;
import edu.wharton.alumni.model.BioBookDirectoryProfile;
import edu.wharton.alumni.model.BioBookProfile;
import edu.wharton.alumni.model.Role;
import edu.wharton.alumni.repository.BioBookClaimEmailRepository;
import edu.wharton.alumni.repository.BioBookClaimRepository;
import edu.wharton.alumni.repository.BioBookDirectoryProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class BioBookService {
    private final AlumniService alumniService;
    private final AuthService authService;
    private final BioBookDirectoryProfileRepository directoryProfileRepository;
    private final BioBookClaimEmailRepository claimEmailRepository;
    private final BioBookClaimRepository claimRepository;
    private final ObjectMapper objectMapper;

    public BioBookService(AlumniService alumniService, AuthService authService,
                          BioBookDirectoryProfileRepository directoryProfileRepository,
                          BioBookClaimEmailRepository claimEmailRepository,
                          BioBookClaimRepository claimRepository, ObjectMapper objectMapper) {
        this.alumniService = alumniService;
        this.authService = authService;
        this.directoryProfileRepository = directoryProfileRepository;
        this.claimEmailRepository = claimEmailRepository;
        this.claimRepository = claimRepository;
        this.objectMapper = objectMapper;
    }

    public BioBookLookupResponse lookup(String email) {
        Optional<BioBookProfile> profile = findProfileByEmail(email);
        return new BioBookLookupResponse(
                profile.isPresent(),
                profile.isPresent() && alumniService.findByEmail(email).isPresent(),
                profile.orElse(null)
        );
    }

    public List<BioBookProfile> findAllBioBookProfiles() {
        Map<String, BioBookProfile> profilesById = new LinkedHashMap<>();
        directoryProfileRepository.findAll().stream()
                .map(this::toBioBookProfile)
                .forEach(profile -> profilesById.put(profileKey(profile), profile));
        claimRepository.findAll().stream()
                .map(this::toBioBookProfile)
                .forEach(profile -> profilesById.putIfAbsent(profileKey(profile), profile));

        return profilesById.values().stream()
                .sorted((left, right) -> valueOr(left.fullLegalName(), "").compareToIgnoreCase(valueOr(right.fullLegalName(), "")))
                .toList();
    }

    public Optional<BioBookProfile> findBioBookProfileById(String id) {
        Optional<BioBookProfile> normalizedProfile = directoryProfileRepository.findBySlug(id)
                .map(this::toBioBookProfile);
        if (normalizedProfile.isPresent()) {
            return normalizedProfile;
        }

        return findAllBioBookProfiles().stream()
                .filter(profile -> profile.id() != null && profile.id().equals(id))
                .findFirst();
    }

    public BioBookClaimResponse claim(String email, String password) {
        BioBookProfile bioBookProfile = findProfileByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No BioBook profile found for that email."));

        if (alumniService.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "An alumni profile already exists for that email. Please log in with your password.");
        }

        AlumniProfile saved = createClaimedAlumniProfile(email, password, bioBookProfile);
        return new BioBookClaimResponse(authService.tokenFor(saved), saved.withoutPassword(), bioBookProfile);
    }

    private Optional<BioBookProfile> findProfileByEmail(String email) {
        return findBioBookProfile(email);
    }

    public Optional<BioBookProfile> findBioBookProfile(String email) {
        String hash = sha256(email.trim().toLowerCase(Locale.ROOT));
        Optional<BioBookProfile> normalizedProfile = claimEmailRepository.findByEmailHash(hash)
                .map(BioBookClaimEmail::profile)
                .map(this::toBioBookProfile);
        if (normalizedProfile.isPresent()) {
            return normalizedProfile;
        }

        return claimRepository.findByEmailHash(hash)
                .map(this::toBioBookProfile);
    }

    private BioBookProfile toBioBookProfile(BioBookDirectoryProfile profile) {
        try {
            return objectMapper.readValue(profile.profileJson(), BioBookProfile.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read BioBook profile " + profile.slug(), exception);
        }
    }

    private BioBookProfile toBioBookProfile(BioBookClaim claim) {
        try {
            return objectMapper.readValue(claim.profileJson(), BioBookProfile.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read BioBook profile for claim " + claim.id(), exception);
        }
    }

    private String profileKey(BioBookProfile profile) {
        String id = profile.id();
        if (id != null && !id.isBlank()) {
            return id;
        }
        return valueOr(profile.batch(), "unknown") + ":" + valueOr(profile.fullLegalName(), "").toLowerCase(Locale.ROOT);
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
                valueOr(bioBookProfile.headshotProfessional(), null),
                toProfileJson(bioBookProfile),
                Role.ALUMNI,
                true,
                Instant.now()
        );
    }

    public AlumniProfile createClaimedAlumniProfile(String email, String password, BioBookProfile bioBookProfile) {
        return alumniService.save(toAlumniProfile(email, password, bioBookProfile));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String toProfileJson(BioBookProfile profile) {
        try {
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to store BioBook profile details.", exception);
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
