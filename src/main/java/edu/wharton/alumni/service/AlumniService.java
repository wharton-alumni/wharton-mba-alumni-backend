package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.ProfileUpdateRequest;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.CohortCampus;
import edu.wharton.alumni.repository.AlumniProfileRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AlumniService {
    private final AlumniProfileRepository profileRepository;
    private final BCryptPasswordEncoder seedEncoder = new BCryptPasswordEncoder();

    public AlumniService(AlumniProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public List<AlumniProfile> findAll(String search, CohortCampus cohortCampus, String industry, String location,
                                       Integer classYearFrom, Integer classYearTo, Boolean willingToMentor, Boolean hiring) {
        return profileRepository.findAll().stream()
                .filter(AlumniProfile::approved)
                .filter(profile -> cohortCampus == null || profile.cohortCampus() == cohortCampus)
                .filter(profile -> blank(industry) || profile.industry().equalsIgnoreCase(industry))
                .filter(profile -> blank(location) || contains(profile.city() + " " + profile.stateCountry(), location))
                .filter(profile -> classYearFrom == null || profile.classYear() >= classYearFrom)
                .filter(profile -> classYearTo == null || profile.classYear() <= classYearTo)
                .filter(profile -> willingToMentor == null || !willingToMentor || profile.willingToMentor())
                .filter(profile -> hiring == null || !hiring || profile.hiring())
                .filter(profile -> blank(search) || searchableText(profile).contains(search.toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(AlumniProfile::lastName).thenComparing(AlumniProfile::firstName))
                .map(AlumniProfile::withoutPassword)
                .toList();
    }

    public Optional<AlumniProfile> findByEmail(String email) {
        return profileRepository.findByEmailIgnoreCase(email);
    }

    public AlumniProfile findInternal(UUID id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Profile not found."));
    }

    public AlumniProfile save(AlumniProfile profile) {
        return profileRepository.save(profile);
    }

    public AlumniProfile update(UUID id, ProfileUpdateRequest request) {
        AlumniProfile current = findInternal(id);
        AlumniProfile updated = new AlumniProfile(
                current.id(),
                current.email(),
                current.passwordHash(),
                current.firstName(),
                current.lastName(),
                valueOr(request.phoneNumber(), current.phoneNumber()),
                current.cohortCampus(),
                current.classYear(),
                valueOr(request.currentTitle(), current.currentTitle()),
                valueOr(request.currentCompany(), current.currentCompany()),
                valueOr(request.industry(), current.industry()),
                valueOr(request.city(), current.city()),
                valueOr(request.stateCountry(), current.stateCountry()),
                valueOr(request.linkedinUrl(), current.linkedinUrl()),
                valueOr(request.bio(), current.bio()),
                request.willingToMentor() == null ? current.willingToMentor() : request.willingToMentor(),
                request.hiring() == null ? current.hiring() : request.hiring(),
                valueOr(request.avatarUrl(), current.avatarUrl()),
                current.role(),
                current.approved(),
                current.createdAt()
        );
        return profileRepository.save(updated).withoutPassword();
    }

    public void replaceWithSeedData(List<SeedAlumniProfile> seedProfiles) {
        profileRepository.deleteAll();
        for (int index = 0; index < seedProfiles.size(); index++) {
            SeedAlumniProfile seed = seedProfiles.get(index);
            UUID id = UUID.nameUUIDFromBytes(seed.email().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            profileRepository.save(new AlumniProfile(
                id,
                seed.email().toLowerCase(Locale.ROOT),
                seedEncoder.encode("password"),
                seed.firstName(),
                seed.lastName(),
                seed.phoneNumber(),
                seed.cohortCampus(),
                seed.classYear(),
                seed.currentTitle(),
                seed.currentCompany(),
                seed.industry(),
                seed.city(),
                seed.stateCountry(),
                seed.linkedinUrl(),
                seed.bio(),
                seed.willingToMentor(),
                seed.hiring(),
                seed.avatarUrl(),
                seed.role(),
                seed.approved(),
                Instant.now().minusSeconds((long) index * 86400)
            ));
        }
    }

    private String searchableText(AlumniProfile profile) {
        List<String> values = new ArrayList<>();
        values.add(profile.firstName());
        values.add(profile.lastName());
        values.add(profile.currentCompany());
        values.add(profile.currentTitle());
        values.add(profile.industry());
        values.add(profile.bio());
        return String.join(" ", values).toLowerCase(Locale.ROOT);
    }

    private boolean contains(String source, String target) {
        return source.toLowerCase(Locale.ROOT).contains(target.toLowerCase(Locale.ROOT));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String valueOr(String next, String current) {
        return next == null ? current : next;
    }
}
