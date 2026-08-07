package edu.wharton.alumni.dto;

import edu.wharton.alumni.model.EventCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull EventCategory category,
        Instant eventDate,
        @NotBlank String location,
        String externalLink,
        String imageUrl,
        @NotNull UUID postedById
) {
}
