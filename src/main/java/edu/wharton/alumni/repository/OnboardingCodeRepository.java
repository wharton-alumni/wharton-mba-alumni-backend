package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.OnboardingCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnboardingCodeRepository extends JpaRepository<OnboardingCode, UUID> {
    Optional<OnboardingCode> findTopByEmailOrderByCreatedAtDesc(String email);
}
