package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {
    List<ConsentRecord> findByProfileIdOrderByAcceptedAtDesc(UUID profileId);
}
