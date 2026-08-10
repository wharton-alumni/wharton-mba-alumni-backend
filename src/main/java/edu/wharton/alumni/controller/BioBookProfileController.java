package edu.wharton.alumni.controller;

import edu.wharton.alumni.model.BioBookProfile;
import edu.wharton.alumni.service.BioBookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/biobook/profiles")
public class BioBookProfileController {
    private final BioBookService bioBookService;

    public BioBookProfileController(BioBookService bioBookService) {
        this.bioBookService = bioBookService;
    }

    @GetMapping
    public List<BioBookProfile> list() {
        return bioBookService.findAllBioBookProfiles();
    }

    @GetMapping("/{id}")
    public BioBookProfile find(@PathVariable String id) {
        return bioBookService.findBioBookProfileById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "BioBook profile not found."));
    }
}
