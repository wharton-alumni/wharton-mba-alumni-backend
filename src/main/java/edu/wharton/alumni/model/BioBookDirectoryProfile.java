package edu.wharton.alumni.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "biobook_profiles")
public class BioBookDirectoryProfile {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String batch;

    @Column(nullable = false, columnDefinition = "text")
    private String profileJson;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected BioBookDirectoryProfile() {
    }

    public BioBookDirectoryProfile(UUID id, String slug, String batch, String profileJson,
                                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.slug = slug;
        this.batch = batch;
        this.profileJson = profileJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public BioBookDirectoryProfile withProfile(String batch, String profileJson) {
        return new BioBookDirectoryProfile(id, slug, batch, profileJson, createdAt, Instant.now());
    }

    public UUID id() {
        return id;
    }

    public String slug() {
        return slug;
    }

    public String batch() {
        return batch;
    }

    public String profileJson() {
        return profileJson;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
