package edu.wharton.alumni.dto;

import edu.wharton.alumni.model.CohortCampus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @Size(min = 6) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phoneNumber,
        @NotNull CohortCampus cohortCampus,
        @Min(1975) @Max(2028) int classYear,
        @NotBlank String currentTitle,
        @NotBlank String currentCompany,
        @NotBlank String industry,
        @NotBlank String city,
        @NotBlank String stateCountry,
        String linkedinUrl,
        @NotBlank String bio,
        boolean willingToMentor,
        boolean hiring,
        String avatarUrl,
        String bioBookProfileJson
) {
}
