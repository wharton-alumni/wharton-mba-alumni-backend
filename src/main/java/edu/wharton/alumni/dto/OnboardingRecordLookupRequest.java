package edu.wharton.alumni.dto;

import jakarta.validation.constraints.NotBlank;

public record OnboardingRecordLookupRequest(
        @NotBlank String fullName,
        String batch,
        String cohort
) {
}
