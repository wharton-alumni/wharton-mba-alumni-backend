package edu.wharton.alumni.dto;

public record OnboardingLookupResponse(
        boolean exists,
        boolean alreadyClaimed,
        String fullLegalName,
        String cohort,
        String batch,
        String currentEmployer,
        String currentTitleRole
) {
}
