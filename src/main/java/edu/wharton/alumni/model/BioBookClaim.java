package edu.wharton.alumni.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "biobook_claims")
public class BioBookClaim {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String emailHash;

    @Column(nullable = false, columnDefinition = "text")
    private String profileJson;

    protected BioBookClaim() {
    }

    public BioBookClaim(UUID id, String emailHash, String profileJson) {
        this.id = id;
        this.emailHash = emailHash;
        this.profileJson = profileJson;
    }

    public UUID id() {
        return id;
    }

    public String emailHash() {
        return emailHash;
    }

    public String profileJson() {
        return profileJson;
    }
}
