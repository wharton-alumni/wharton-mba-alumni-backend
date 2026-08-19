package edu.wharton.alumni.dto;

public record OnboardingRecordLookupResponse(
        boolean found,
        String destination
) {
}
