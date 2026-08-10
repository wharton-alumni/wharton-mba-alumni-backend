package edu.wharton.alumni.dto;

public record ProfileUpdateRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        edu.wharton.alumni.model.CohortCampus cohortCampus,
        Integer classYear,
        String currentTitle,
        String currentCompany,
        String industry,
        String city,
        String stateCountry,
        String linkedinUrl,
        String bio,
        Boolean willingToMentor,
        Boolean hiring,
        String avatarUrl
) {
}
