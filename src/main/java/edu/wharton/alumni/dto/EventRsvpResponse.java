package edu.wharton.alumni.dto;

import edu.wharton.alumni.model.EventRsvpStatus;

import java.time.Instant;
import java.util.UUID;

public record EventRsvpResponse(
        UUID eventId,
        UUID profileId,
        EventRsvpStatus status,
        long joinedCount,
        long interestedCount,
        Instant updatedAt
) {
}
