package edu.wharton.alumni.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "biobook_claim_emails")
public class BioBookClaimEmail {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private BioBookDirectoryProfile profile;

    @Column(nullable = false, unique = true, length = 64)
    private String emailHash;

    @Column(nullable = false)
    private Instant createdAt;

    protected BioBookClaimEmail() {
    }

    public BioBookClaimEmail(UUID id, BioBookDirectoryProfile profile, String emailHash, Instant createdAt) {
        this.id = id;
        this.profile = profile;
        this.emailHash = emailHash;
        this.createdAt = createdAt;
    }

    public BioBookClaimEmail withProfile(BioBookDirectoryProfile profile) {
        return new BioBookClaimEmail(id, profile, emailHash, createdAt);
    }

    public UUID id() {
        return id;
    }

    public BioBookDirectoryProfile profile() {
        return profile;
    }

    public String emailHash() {
        return emailHash;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
