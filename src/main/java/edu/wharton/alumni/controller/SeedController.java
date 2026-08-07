package edu.wharton.alumni.controller;

import edu.wharton.alumni.service.SeedDataService;
import edu.wharton.alumni.service.SeedResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/seed")
public class SeedController {
    private final SeedDataService seedDataService;

    public SeedController(SeedDataService seedDataService) {
        this.seedDataService = seedDataService;
    }

    @PostMapping
    public SeedResult seed() {
        return seedDataService.seed();
    }
}
