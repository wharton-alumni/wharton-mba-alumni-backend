package edu.wharton.alumni.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_records")
public class ConsentRecord {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID profileId;

    @Column(nullable = false, length = 5000)
    private String consentText;

    @Column(nullable = false)
    private Instant acceptedAt;

    protected ConsentRecord() {
    }

    public ConsentRecord(UUID id, UUID profileId, String consentText, Instant acceptedAt) {
        this.id = id;
        this.profileId = profileId;
        this.consentText = consentText;
        this.acceptedAt = acceptedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID profileId() {
        return profileId;
    }

    public String consentText() {
        return consentText;
    }

    public Instant acceptedAt() {
        return acceptedAt;
    }
}
