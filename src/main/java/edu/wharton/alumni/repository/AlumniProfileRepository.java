package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.AlumniProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlumniProfileRepository extends JpaRepository<AlumniProfile, UUID> {
    Optional<AlumniProfile> findByEmailIgnoreCase(String email);

    List<AlumniProfile> findByIdInAndApprovedTrue(List<UUID> ids);

    boolean existsByEmailIgnoreCase(String email);
}
