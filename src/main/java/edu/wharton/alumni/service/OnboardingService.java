package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.LoginResponse;
import edu.wharton.alumni.dto.OnboardingClaimRequest;
import edu.wharton.alumni.dto.OnboardingLookupRequest;
import edu.wharton.alumni.dto.OnboardingLookupResponse;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.BioBookProfile;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class OnboardingService {
    private final BioBookService bioBookService;
    private final AlumniService alumniService;
    private final AuthService authService;

    public OnboardingService(BioBookService bioBookService, AlumniService alumniService, AuthService authService) {
        this.bioBookService = bioBookService;
        this.alumniService = alumniService;
        this.authService = authService;
    }

    public OnboardingLookupResponse lookup(OnboardingLookupRequest request) {
        Optional<BioBookProfile> profile = bioBookService.findBioBookProfile(request.email());
        if (profile.isEmpty()) {
            return new OnboardingLookupResponse(false, false, null, null, null, null, null);
        }
        BioBookProfile bioBookProfile = profile.get();
        return new OnboardingLookupResponse(
                true,
                alumniService.findByEmail(request.email()).isPresent(),
                bioBookProfile.fullLegalName(),
                bioBookProfile.cohort(),
                bioBookProfile.batch(),
                bioBookProfile.currentEmployer(),
                bioBookProfile.currentTitleRole()
        );
    }

    public LoginResponse claim(OnboardingClaimRequest request) {
        String email = normalize(request.email());
        BioBookProfile bioBookProfile = bioBookService.findBioBookProfile(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No BioBook profile found for that work email."));
        if (alumniService.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "An alumni profile already exists for that email.");
        }
        AlumniProfile profile = bioBookService.createClaimedAlumniProfile(email, request.password(), bioBookProfile);
        return new LoginResponse(authService.tokenFor(profile), profile.withoutPassword());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
