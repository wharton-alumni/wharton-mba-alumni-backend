package edu.wharton.alumni.dto;

import jakarta.validation.constraints.NotBlank;

public record OnboardingRecordVerifyRequest(
        @NotBlank String fullName,
        @NotBlank String code
) {
}
