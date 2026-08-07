package edu.wharton.alumni.dto;

import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.BioBookProfile;

public record BioBookClaimResponse(
        String token,
        AlumniProfile profile,
        BioBookProfile biobookProfile
) {
}
