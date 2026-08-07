package edu.wharton.alumni.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OnboardingLookupRequest(@Email @NotBlank String email) {
}
