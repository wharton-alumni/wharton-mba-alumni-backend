package edu.wharton.alumni.model;

import java.time.Instant;
import java.util.UUID;

public record AlumniEvent(
        UUID id,
        String title,
        String description,
        EventCategory category,
        Instant eventDate,
        String location,
        String externalLink,
        String imageUrl,
        UUID postedById,
        String postedByName,
        CohortCampus postedByCohort,
        EventStatus status,
        Instant createdAt
) {
}
