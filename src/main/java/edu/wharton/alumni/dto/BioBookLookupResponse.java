package edu.wharton.alumni.dto;

import edu.wharton.alumni.model.BioBookProfile;

public record BioBookLookupResponse(
        boolean exists,
        BioBookProfile profile
) {
}
