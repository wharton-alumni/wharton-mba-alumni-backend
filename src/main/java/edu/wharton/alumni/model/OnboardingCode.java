package edu.wharton.alumni.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "onboarding_codes")
public class OnboardingCode {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant verifiedAt;
    private Instant createdAt;

    protected OnboardingCode() {
    }

    public OnboardingCode(UUID id, String email, String codeHash, Instant expiresAt, Instant verifiedAt, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
    }

    public String email() {
        return email;
    }

    public String codeHash() {
        return codeHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant verifiedAt() {
        return verifiedAt;
    }

    public OnboardingCode markVerified() {
        return new OnboardingCode(id, email, codeHash, expiresAt, Instant.now(), createdAt);
    }
}
