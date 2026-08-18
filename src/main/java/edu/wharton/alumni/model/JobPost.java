package edu.wharton.alumni.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_posts")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class JobPost {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String company;
    private String location;
    private String externalLink;
    private String applicationLink;
    @Column(length = 5000)
    private String description;
    private UUID postedById;
    private String postedByName;
    private Instant createdAt;
    private Instant startDate;
    private Instant endDate;

    protected JobPost() {
    }

    public JobPost(UUID id, String title, String company, String location, String externalLink, String applicationLink,
                   String description, UUID postedById, String postedByName, Instant createdAt) {
        this(id, title, company, location, externalLink, applicationLink, description, postedById, postedByName, createdAt, null, null);
    }

    public JobPost(UUID id, String title, String company, String location, String externalLink, String applicationLink,
                   String description, UUID postedById, String postedByName, Instant createdAt,
                   Instant startDate, Instant endDate) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.externalLink = externalLink;
        this.applicationLink = applicationLink;
        this.description = description;
        this.postedById = postedById;
        this.postedByName = postedByName;
        this.createdAt = createdAt;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID id() { return id; }

    public String postedByName() { return postedByName; }

    public UUID postedById() { return postedById; }

    public Instant createdAt() { return createdAt; }

    public Instant endDate() { return endDate; }
}
