package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.EventRequest;
import edu.wharton.alumni.model.AlumniEvent;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.EventStatus;
import edu.wharton.alumni.repository.AlumniEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class EventService {
    private final AlumniEventRepository eventRepository;
    private final AlumniService alumniService;

    public EventService(AlumniEventRepository eventRepository, AlumniService alumniService) {
        this.eventRepository = eventRepository;
        this.alumniService = alumniService;
    }

    public List<AlumniEvent> findByStatus(EventStatus status) {
        if (status == null) {
            return eventRepository.findAllByOrderByCreatedAtDesc();
        }
        return eventRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public AlumniEvent submit(EventRequest request) {
        AlumniProfile poster = alumniService.findInternal(request.postedById());
        AlumniEvent event = new AlumniEvent(
                UUID.randomUUID(),
                request.title(),
                request.description(),
                request.category(),
                request.eventDate(),
                request.location(),
                request.externalLink(),
                request.imageUrl(),
                poster.id(),
                poster.firstName() + " " + poster.lastName(),
                poster.cohortCampus(),
                EventStatus.PENDING,
                Instant.now()
        );
        return eventRepository.save(event);
    }

    public AlumniEvent updateStatus(UUID id, EventStatus status) {
        if (status == EventStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Admin moderation must approve or reject a submission.");
        }
        AlumniEvent current = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Event not found."));
        AlumniEvent updated = new AlumniEvent(current.id(), current.title(), current.description(), current.category(),
                current.eventDate(), current.location(), current.externalLink(), current.imageUrl(), current.postedById(),
                current.postedByName(), current.postedByCohort(), status, current.createdAt());
        return eventRepository.save(updated);
    }

    public void replaceWithSeedData(List<SeedEvent> seedEvents) {
        eventRepository.deleteAll();
        for (int index = 0; index < seedEvents.size(); index++) {
            SeedEvent seed = seedEvents.get(index);
            AlumniProfile poster = alumniService.findByEmail(seed.postedByEmail()).orElseThrow();
            UUID id = UUID.nameUUIDFromBytes((seed.title() + seed.postedByEmail()).getBytes());
            eventRepository.save(new AlumniEvent(id, seed.title(), seed.description(), seed.category(), seed.eventDate(),
                    seed.location(), seed.externalLink(), seed.imageUrl(), poster.id(),
                    poster.firstName() + " " + poster.lastName(), poster.cohortCampus(), seed.status(),
                    Instant.now().minusSeconds((long) index * 3600)));
        }
    }
}
