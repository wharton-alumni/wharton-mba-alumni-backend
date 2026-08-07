package edu.wharton.alumni.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Announcement {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, length = 5000)
    private String body;
    private boolean published;
    private Instant createdAt;
    private Instant updatedAt;

    protected Announcement() {
    }

    public Announcement(UUID id, String title, String body, boolean published, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.published = published;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID id() { return id; }
    public String title() { return title; }
    public String body() { return body; }
    public boolean published() { return published; }
    public Instant createdAt() { return createdAt; }
}
