package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.BioBookClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BioBookClaimRepository extends JpaRepository<BioBookClaim, UUID> {
    Optional<BioBookClaim> findByEmailHash(String emailHash);
}
