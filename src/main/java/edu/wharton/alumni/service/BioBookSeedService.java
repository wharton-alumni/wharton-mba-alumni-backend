package edu.wharton.alumni.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wharton.alumni.model.BioBookClaim;
import edu.wharton.alumni.repository.BioBookClaimRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class BioBookSeedService {
    private final ObjectMapper objectMapper;
    private final BioBookClaimRepository bioBookClaimRepository;
    private final boolean seedEnabled;

    public BioBookSeedService(ObjectMapper objectMapper,
                              BioBookClaimRepository bioBookClaimRepository,
                              @Value("${app.biobook.seed.enabled}") boolean seedEnabled) {
        this.objectMapper = objectMapper;
        this.bioBookClaimRepository = bioBookClaimRepository;
        this.seedEnabled = seedEnabled;
    }

    @PostConstruct
    public void seedOnStartup() {
        if (seedEnabled && bioBookClaimRepository.count() == 0) {
            seedMissing();
        }
    }

    public int replaceAll() throws IOException {
        List<BioBookClaimRecord> records = readSeedRecords();
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

    private List<BioBookClaimRecord> readSeedRecords() throws IOException {
        return objectMapper.readValue(
                new ClassPathResource("seed/biobook-claims.json").getInputStream(),
                new TypeReference<>() {
                }
        );
    }

    private void save(List<BioBookClaimRecord> records) throws IOException {
        for (BioBookClaimRecord record : records) {
            String profileJson = objectMapper.writeValueAsString(record.profile());
            for (String emailHash : record.emailHashes()) {
                UUID id = UUID.nameUUIDFromBytes(emailHash.getBytes(StandardCharsets.UTF_8));
                if (!bioBookClaimRepository.existsById(id)) {
                    bioBookClaimRepository.save(new BioBookClaim(id, emailHash, profileJson));
                }
            }
        }
    }
}
