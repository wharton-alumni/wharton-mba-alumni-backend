package edu.wharton.alumni.dto;

import edu.wharton.alumni.model.AlumniProfile;

public record LoginResponse(
        String token,
        AlumniProfile profile
) {
}
