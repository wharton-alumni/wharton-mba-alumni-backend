package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.ProfileUpdateRequest;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.CohortCampus;
import edu.wharton.alumni.security.JwtUser;
import edu.wharton.alumni.service.AlumniService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final AlumniService alumniService;

    public ProfileController(AlumniService alumniService) {
        this.alumniService = alumniService;
    }

    @GetMapping("/me")
    public AlumniProfile me(@AuthenticationPrincipal JwtUser user) {
        return alumniService.findInternal(user.id()).withoutPassword();
    }

    @PutMapping("/me")
    public AlumniProfile updateMe(@AuthenticationPrincipal JwtUser user, @RequestBody ProfileUpdateRequest request) {
        return alumniService.update(user.id(), request);
    }

    @GetMapping
    public List<AlumniProfile> listProfiles(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CohortCampus cohortCampus,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer classYearFrom,
            @RequestParam(required = false) Integer classYearTo,
            @RequestParam(required = false) Boolean willingToMentor,
            @RequestParam(required = false) Boolean hiring
    ) {
        return alumniService.findAll(search, cohortCampus, industry, location, classYearFrom, classYearTo, willingToMentor, hiring);
    }

    @GetMapping("/{id}")
    public AlumniProfile findProfile(@PathVariable UUID id) {
        return alumniService.findPublic(id);
    }

    @PutMapping("/{id}")
    public AlumniProfile updateProfile(@PathVariable UUID id, @RequestBody ProfileUpdateRequest request) {
        return alumniService.update(id, request);
    }
}
