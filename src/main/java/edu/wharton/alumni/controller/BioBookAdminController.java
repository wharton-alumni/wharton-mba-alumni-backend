package edu.wharton.alumni.controller;

import edu.wharton.alumni.service.BioBookClaimRecord;
import edu.wharton.alumni.service.BioBookSeedService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/biobook")
public class BioBookAdminController {
    private final BioBookSeedService bioBookSeedService;

    public BioBookAdminController(BioBookSeedService bioBookSeedService) {
        this.bioBookSeedService = bioBookSeedService;
    }

    @PostMapping("/import")
    public BioBookImportResponse importProfiles(@RequestBody List<BioBookClaimRecord> records) {
        return new BioBookImportResponse(bioBookSeedService.importRecords(records));
    }

    public record BioBookImportResponse(int profilesImported) {
    }
}
