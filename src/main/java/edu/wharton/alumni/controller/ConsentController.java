package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.ConsentRequest;
import edu.wharton.alumni.model.ConsentRecord;
import edu.wharton.alumni.security.JwtUser;
import edu.wharton.alumni.service.ConsentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/consents")
public class ConsentController {
    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    public ConsentRecord accept(@AuthenticationPrincipal JwtUser user, @Valid @RequestBody ConsentRequest request) {
        return consentService.accept(user.id(), request);
    }

    @GetMapping("/me")
    public List<ConsentRecord> mine(@AuthenticationPrincipal JwtUser user) {
        return consentService.findMine(user.id());
    }
}
