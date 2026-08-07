package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.ConsentRequest;
import edu.wharton.alumni.model.ConsentRecord;
import edu.wharton.alumni.repository.ConsentRecordRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ConsentService {
    private final ConsentRecordRepository consentRecordRepository;

    public ConsentService(ConsentRecordRepository consentRecordRepository) {
        this.consentRecordRepository = consentRecordRepository;
    }

    public ConsentRecord accept(UUID profileId, ConsentRequest request) {
        return consentRecordRepository.save(new ConsentRecord(UUID.randomUUID(), profileId, request.consentText(), Instant.now()));
    }

    public List<ConsentRecord> findMine(UUID profileId) {
        return consentRecordRepository.findByProfileIdOrderByAcceptedAtDesc(profileId);
    }
}
