package edu.wharton.alumni.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SeedDataService {
    private final ObjectMapper objectMapper;
    private final AlumniService alumniService;
    private final EventService eventService;
    private final boolean seedEnabled;

    public SeedDataService(ObjectMapper objectMapper, AlumniService alumniService, EventService eventService,
                           @Value("${app.seed.enabled}") boolean seedEnabled) {
        this.objectMapper = objectMapper;
        this.alumniService = alumniService;
        this.eventService = eventService;
        this.seedEnabled = seedEnabled;
    }

    @PostConstruct
    public void seedOnStartup() {
        if (seedEnabled) {
            seed();
        }
    }

    public SeedResult seed() {
        try {
            List<SeedAlumniProfile> alumni = objectMapper.readValue(
                    new ClassPathResource("seed/alumni-profiles.json").getInputStream(),
                    new TypeReference<>() {
                    }
            );
            List<SeedEvent> events = objectMapper.readValue(
                    new ClassPathResource("seed/events.json").getInputStream(),
                    new TypeReference<>() {
                    }
            );
            alumniService.replaceWithSeedData(alumni);
            eventService.replaceWithSeedData(events);
            return new SeedResult(alumni.size(), events.size());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load seed data.", exception);
        }
    }
}
