package edu.wharton.alumni.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "external_event_sync_state")
public class ExternalEventSyncState {
    @Id
    private String id;

    private Instant lastScrapedAt;
    private Instant lastDbUpdatedAt;

    @Column(columnDefinition = "text")
    private String lastError;

    protected ExternalEventSyncState() {
    }

    public ExternalEventSyncState(String id, Instant lastScrapedAt, Instant lastDbUpdatedAt, String lastError) {
        this.id = id;
        this.lastScrapedAt = lastScrapedAt;
        this.lastDbUpdatedAt = lastDbUpdatedAt;
        this.lastError = lastError;
    }

    public String id() {
        return id;
    }

    public Instant lastScrapedAt() {
        return lastScrapedAt;
    }

    public Instant lastDbUpdatedAt() {
        return lastDbUpdatedAt;
    }

    public String lastError() {
        return lastError;
    }
}
