package edu.wharton.alumni.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record JobRequest(
        @NotBlank String title,
        @NotBlank String company,
        String location,
        String externalLink,
        String applicationLink,
        @NotBlank String description,
        Instant startDate,
        Instant endDate
) {
}
