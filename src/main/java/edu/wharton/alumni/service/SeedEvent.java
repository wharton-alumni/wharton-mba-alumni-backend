package edu.wharton.alumni.service;

import edu.wharton.alumni.model.EventCategory;
import edu.wharton.alumni.model.EventStatus;

import java.time.Instant;

public record SeedEvent(
        String title,
        String description,
        EventCategory category,
        Instant eventDate,
        String location,
        String externalLink,
        String imageUrl,
        String postedByEmail,
        EventStatus status
) {
}
