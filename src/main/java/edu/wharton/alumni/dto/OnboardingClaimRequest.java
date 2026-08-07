package edu.wharton.alumni.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingClaimRequest(
        @Email @NotBlank String email,
        @NotBlank String code,
        @Size(min = 8) String password
) {
}
