package edu.wharton.alumni.dto;

import jakarta.validation.constraints.NotBlank;

public record JobRequest(
        @NotBlank String title,
        @NotBlank String company,
        String location,
        String externalLink,
        String applicationLink,
        @NotBlank String description
) {
}
