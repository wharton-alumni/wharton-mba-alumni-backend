package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.EventRsvpResponse;
import edu.wharton.alumni.model.EventRsvp;
import edu.wharton.alumni.model.EventRsvpStatus;
import edu.wharton.alumni.repository.AlumniEventRepository;
import edu.wharton.alumni.repository.EventRsvpRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class EventRsvpService {
    private final EventRsvpRepository rsvpRepository;
    private final AlumniEventRepository eventRepository;

    public EventRsvpService(EventRsvpRepository rsvpRepository, AlumniEventRepository eventRepository) {
        this.rsvpRepository = rsvpRepository;
        this.eventRepository = eventRepository;
    }

    public List<EventRsvpResponse> findForProfile(UUID profileId) {
        return rsvpRepository.findByProfileId(profileId).stream()
                .map(this::toResponse)
                .toList();
    }

    public EventRsvpResponse update(UUID eventId, UUID profileId, EventRsvpStatus status) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(NOT_FOUND, "Event not found.");
        }

        EventRsvp current = rsvpRepository.findByEventIdAndProfileId(eventId, profileId)
                .orElse(null);
        EventRsvp next = new EventRsvp(
                current == null ? UUID.randomUUID() : current.id(),
                eventId,
                profileId,
                status,
                Instant.now()
        );
        return toResponse(rsvpRepository.save(next));
    }

    private EventRsvpResponse toResponse(EventRsvp rsvp) {
        return new EventRsvpResponse(
                rsvp.eventId(),
                rsvp.profileId(),
                rsvp.status(),
                rsvpRepository.countByEventIdAndStatus(rsvp.eventId(), EventRsvpStatus.JOINED),
                rsvpRepository.countByEventIdAndStatus(rsvp.eventId(), EventRsvpStatus.INTERESTED),
                rsvp.updatedAt()
        );
    }
}
