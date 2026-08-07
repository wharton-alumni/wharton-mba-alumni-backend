package edu.wharton.alumni.dto;

import jakarta.validation.constraints.NotBlank;

public record AnnouncementRequest(
        @NotBlank String title,
        @NotBlank String body,
        boolean published
) {
}
