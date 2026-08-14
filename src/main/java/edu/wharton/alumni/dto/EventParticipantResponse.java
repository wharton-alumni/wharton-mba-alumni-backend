package edu.wharton.alumni.dto;

import edu.wharton.alumni.model.CohortCampus;

import java.time.Instant;
import java.util.UUID;

public record EventParticipantResponse(
        UUID profileId,
        String fullName,
        String currentTitle,
        String currentCompany,
        CohortCampus cohortCampus,
        int classYear,
        String avatarUrl,
        Instant joinedAt
) {
}
