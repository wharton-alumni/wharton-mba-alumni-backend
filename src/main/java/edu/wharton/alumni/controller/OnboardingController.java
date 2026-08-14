package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.LoginResponse;
import edu.wharton.alumni.dto.OnboardingClaimRequest;
import edu.wharton.alumni.dto.OnboardingLookupRequest;
import edu.wharton.alumni.dto.OnboardingLookupResponse;
import edu.wharton.alumni.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {
    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/lookup")
    public OnboardingLookupResponse lookup(@Valid @RequestBody OnboardingLookupRequest request) {
        return onboardingService.lookup(request);
    }

    @PostMapping("/claim")
    public LoginResponse claim(@Valid @RequestBody OnboardingClaimRequest request) {
        return onboardingService.claim(request);
    }
}
