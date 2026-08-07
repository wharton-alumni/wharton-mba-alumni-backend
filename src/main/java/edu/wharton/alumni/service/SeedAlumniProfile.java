package edu.wharton.alumni.service;

import edu.wharton.alumni.model.CohortCampus;
import edu.wharton.alumni.model.Role;

public record SeedAlumniProfile(
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        CohortCampus cohortCampus,
        int classYear,
        String currentTitle,
        String currentCompany,
        String industry,
        String city,
        String stateCountry,
        String linkedinUrl,
        String bio,
        boolean willingToMentor,
        boolean hiring,
        String avatarUrl,
        Role role,
        boolean approved
) {
}
