package edu.wharton.alumni.model;

import java.time.Instant;
import java.util.UUID;

public record AlumniProfile(
        UUID id,
        String email,
        String passwordHash,
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
        boolean approved,
        Instant createdAt
) {
    public AlumniProfile withoutPassword() {
        return new AlumniProfile(id, email, null, firstName, lastName, phoneNumber, cohortCampus, classYear,
                currentTitle, currentCompany, industry, city, stateCountry, linkedinUrl, bio, willingToMentor,
                hiring, avatarUrl, role, approved, createdAt);
    }
}
