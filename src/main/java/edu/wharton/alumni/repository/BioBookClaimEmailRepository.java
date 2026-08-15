package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.BioBookClaimEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BioBookClaimEmailRepository extends JpaRepository<BioBookClaimEmail, UUID> {
    Optional<BioBookClaimEmail> findByEmailHash(String emailHash);
}
