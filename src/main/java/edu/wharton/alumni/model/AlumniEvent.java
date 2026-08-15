package edu.wharton.alumni.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alumni_events")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class AlumniEvent {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    private Instant eventDate;
    private String location;
    private String externalLink;
    private String imageUrl;

    @Column(nullable = false)
    private UUID postedById;

    private String postedByName;

    @Enumerated(EnumType.STRING)
    private CohortCampus postedByCohort;

    private boolean onlyMyBatchCanJoin;

    private Integer allowedClassYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected AlumniEvent() {
    }

    public AlumniEvent(UUID id, String title, String description, EventCategory category, Instant eventDate,
                       String location, String externalLink, String imageUrl, UUID postedById, String postedByName,
                       CohortCampus postedByCohort, boolean onlyMyBatchCanJoin, Integer allowedClassYear,
                       EventStatus status, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.eventDate = eventDate;
        this.location = location;
        this.externalLink = externalLink;
        this.imageUrl = imageUrl;
        this.postedById = postedById;
        this.postedByName = postedByName;
        this.postedByCohort = postedByCohort;
        this.onlyMyBatchCanJoin = onlyMyBatchCanJoin;
        this.allowedClassYear = allowedClassYear;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public EventCategory category() {
        return category;
    }

    public Instant eventDate() {
        return eventDate;
    }

    public String location() {
        return location;
    }

    public String externalLink() {
        return externalLink;
    }

    public String imageUrl() {
        return imageUrl;
    }

    public UUID postedById() {
        return postedById;
    }

    public String postedByName() {
        return postedByName;
    }

    public CohortCampus postedByCohort() {
        return postedByCohort;
    }

    public boolean onlyMyBatchCanJoin() {
        return onlyMyBatchCanJoin;
    }

    public Integer allowedClassYear() {
        return allowedClassYear;
    }

    public EventStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
