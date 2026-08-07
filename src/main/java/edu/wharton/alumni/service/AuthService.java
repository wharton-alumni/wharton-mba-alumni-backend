package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.LoginRequest;
import edu.wharton.alumni.dto.LoginResponse;
import edu.wharton.alumni.dto.RegisterRequest;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.Role;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {
    private final AlumniService alumniService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AlumniService alumniService) {
        this.alumniService = alumniService;
    }

    public LoginResponse login(LoginRequest request) {
        AlumniProfile profile = alumniService.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), profile.passwordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid email or password.");
        }

        return new LoginResponse("demo-token-" + profile.id(), profile.withoutPassword());
    }

    public LoginResponse register(RegisterRequest request) {
        Optional<AlumniProfile> existing = alumniService.findByEmail(request.email());
        if (existing.isPresent()) {
            throw new ResponseStatusException(CONFLICT, "An alumni profile already exists for that email.");
        }

        AlumniProfile profile = new AlumniProfile(
                UUID.randomUUID(),
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.cohortCampus(),
                request.classYear(),
                request.currentTitle(),
                request.currentCompany(),
                request.industry(),
                request.city(),
                request.stateCountry(),
                request.linkedinUrl(),
                request.bio(),
                request.willingToMentor(),
                request.hiring(),
                request.avatarUrl(),
                Role.ALUMNI,
                true,
                Instant.now()
        );
        AlumniProfile saved = alumniService.save(profile);
        return new LoginResponse("demo-token-" + saved.id(), saved.withoutPassword());
    }

    public BCryptPasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }
}
