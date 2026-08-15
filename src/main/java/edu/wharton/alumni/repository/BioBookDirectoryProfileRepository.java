package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.BioBookDirectoryProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BioBookDirectoryProfileRepository extends JpaRepository<BioBookDirectoryProfile, UUID> {
    Optional<BioBookDirectoryProfile> findBySlug(String slug);
}
