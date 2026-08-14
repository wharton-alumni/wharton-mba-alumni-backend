package edu.wharton.alumni.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_rsvps", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "profile_id"})
})
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class EventRsvp {
    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventRsvpStatus status;

    @Column(nullable = false)
    private Instant updatedAt;

    protected EventRsvp() {
    }

    public EventRsvp(UUID id, UUID eventId, UUID profileId, EventRsvpStatus status, Instant updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.profileId = profileId;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public UUID profileId() {
        return profileId;
    }

    public EventRsvpStatus status() {
        return status;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
