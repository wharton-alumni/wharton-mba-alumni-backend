package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.AlumniProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlumniProfileRepository extends JpaRepository<AlumniProfile, UUID> {
    Optional<AlumniProfile> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
