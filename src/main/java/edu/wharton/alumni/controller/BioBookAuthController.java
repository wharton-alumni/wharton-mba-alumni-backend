package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.BioBookClaimRequest;
import edu.wharton.alumni.dto.BioBookClaimResponse;
import edu.wharton.alumni.dto.BioBookLookupRequest;
import edu.wharton.alumni.dto.BioBookLookupResponse;
import edu.wharton.alumni.service.BioBookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/biobook")
public class BioBookAuthController {
    private final BioBookService bioBookService;

    public BioBookAuthController(BioBookService bioBookService) {
        this.bioBookService = bioBookService;
    }

    @PostMapping("/lookup")
    public BioBookLookupResponse lookup(@Valid @RequestBody BioBookLookupRequest request) {
        return bioBookService.lookup(request.email());
    }

    @PostMapping("/claim")
    public BioBookClaimResponse claim(@Valid @RequestBody BioBookClaimRequest request) {
        return bioBookService.claim(request.email(), request.password());
    }
}
