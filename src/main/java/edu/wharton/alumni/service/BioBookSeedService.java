package edu.wharton.alumni.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wharton.alumni.model.BioBookClaimEmail;
import edu.wharton.alumni.model.BioBookClaim;
import edu.wharton.alumni.model.BioBookDirectoryProfile;
import edu.wharton.alumni.model.BioBookProfile;
import edu.wharton.alumni.repository.BioBookClaimEmailRepository;
import edu.wharton.alumni.repository.BioBookClaimRepository;
import edu.wharton.alumni.repository.BioBookDirectoryProfileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BioBookSeedService {
    private final ObjectMapper objectMapper;
    private final BioBookDirectoryProfileRepository directoryProfileRepository;
    private final BioBookClaimEmailRepository claimEmailRepository;
    private final BioBookClaimRepository bioBookClaimRepository;
    private final boolean seedEnabled;

    public BioBookSeedService(ObjectMapper objectMapper,
                              BioBookDirectoryProfileRepository directoryProfileRepository,
                              BioBookClaimEmailRepository claimEmailRepository,
                              BioBookClaimRepository bioBookClaimRepository,
                              @Value("${app.biobook.seed.enabled}") boolean seedEnabled) {
        this.objectMapper = objectMapper;
        this.directoryProfileRepository = directoryProfileRepository;
        this.claimEmailRepository = claimEmailRepository;
        this.bioBookClaimRepository = bioBookClaimRepository;
        this.seedEnabled = seedEnabled;
    }

    @PostConstruct
    public void seedOnStartup() {
        if (seedEnabled) {
            seedMissing();
        }
    }

    public int replaceAll() throws IOException {
        List<BioBookClaimRecord> records = readSeedRecords();
        claimEmailRepository.deleteAll();
        directoryProfileRepository.deleteAll();
        bioBookClaimRepository.deleteAll();
        save(records);
        return records.size();
    }

    public int seedMissing() {
        try {
            List<BioBookClaimRecord> records = readSeedRecords();
            save(records);
            return records.size();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load BioBook seed data.", exception);
        }
    }

    public int importRecords(List<BioBookClaimRecord> records) {
        try {
            save(records);
            return records.size();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to import BioBook records.", exception);
        }
    }

    private List<BioBookClaimRecord> readSeedRecords() throws IOException {
        return objectMapper.readValue(
                new ClassPathResource("seed/biobook-claims.json").getInputStream(),
                new TypeReference<>() {
                }
        );
    }

    private void save(List<BioBookClaimRecord> records) throws IOException {
        for (BioBookClaimRecord record : records) {
            BioBookProfile profile = record.profile();
            String slug = canonicalSlug(profile);
            String profileJson = objectMapper.writeValueAsString(profile);
            String batch = valueOr(profile.batch(), "Unknown");
            UUID profileId = deterministicUuid("biobook-profile:" + slug);
            Instant now = Instant.now();
            BioBookDirectoryProfile directoryProfile = directoryProfileRepository.findBySlug(slug)
                    .map(existing -> existing.withProfile(batch, profileJson))
                    .orElseGet(() -> new BioBookDirectoryProfile(profileId, slug, batch, profileJson, now, now));
            BioBookDirectoryProfile savedDirectoryProfile = directoryProfileRepository.save(directoryProfile);

            for (String emailHash : record.emailHashes()) {
                if (emailHash == null || emailHash.isBlank()) {
                    continue;
                }
                String normalizedEmailHash = emailHash.trim().toLowerCase(Locale.ROOT);
                BioBookClaimEmail claimEmail = claimEmailRepository.findByEmailHash(normalizedEmailHash)
                        .map(existing -> existing.withProfile(savedDirectoryProfile))
                        .orElseGet(() -> new BioBookClaimEmail(
                            deterministicUuid("biobook-claim-email:" + normalizedEmailHash),
                            savedDirectoryProfile,
                            normalizedEmailHash,
                            now
                    ));
                claimEmailRepository.save(claimEmail);

                UUID id = deterministicUuid(normalizedEmailHash);
                bioBookClaimRepository.save(new BioBookClaim(id, normalizedEmailHash, profileJson));
            }
        }
    }

    private UUID deterministicUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private String canonicalSlug(BioBookProfile profile) {
        String id = profile.id();
        if (id != null && !id.isBlank()) {
            return id.trim().toLowerCase(Locale.ROOT);
        }
        throw new IllegalArgumentException("BioBook profiles must include a canonical id.");
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
