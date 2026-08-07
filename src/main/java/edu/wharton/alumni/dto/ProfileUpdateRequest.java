package edu.wharton.alumni.dto;

public record ProfileUpdateRequest(
        String phoneNumber,
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
