package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.LoginResponse;
import edu.wharton.alumni.dto.OnboardingClaimRequest;
import edu.wharton.alumni.dto.OnboardingLookupRequest;
import edu.wharton.alumni.dto.OnboardingLookupResponse;
import edu.wharton.alumni.dto.OnboardingRecordLookupRequest;
import edu.wharton.alumni.dto.SendCodeResponse;
import edu.wharton.alumni.dto.VerifyCodeRequest;
import edu.wharton.alumni.dto.BioBookLookupResponse;
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

    @PostMapping("/send-code")
    public SendCodeResponse sendCode(@Valid @RequestBody OnboardingLookupRequest request) {
        return onboardingService.sendCode(request);
    }

    @PostMapping("/verify-code")
    public SendCodeResponse verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        return onboardingService.verifyCode(request);
    }

    @PostMapping("/find-record")
    public BioBookLookupResponse findRecord(@Valid @RequestBody OnboardingRecordLookupRequest request) {
        return onboardingService.findRecord(request);
    }

    @PostMapping("/claim")
    public LoginResponse claim(@Valid @RequestBody OnboardingClaimRequest request) {
        return onboardingService.claim(request);
    }
}
