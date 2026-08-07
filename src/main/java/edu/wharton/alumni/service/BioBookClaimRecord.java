package edu.wharton.alumni.service;

import edu.wharton.alumni.model.BioBookProfile;

import java.util.List;

public record BioBookClaimRecord(
        List<String> emailHashes,
        BioBookProfile profile
) {
}
