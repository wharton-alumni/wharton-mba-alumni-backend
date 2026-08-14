package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.EventParticipantResponse;
import edu.wharton.alumni.dto.EventRsvpResponse;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.EventRsvp;
import edu.wharton.alumni.model.EventRsvpStatus;
import edu.wharton.alumni.repository.AlumniEventRepository;
import edu.wharton.alumni.repository.EventRsvpRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class EventRsvpService {
    private final EventRsvpRepository rsvpRepository;
    private final AlumniEventRepository eventRepository;
    private final AlumniService alumniService;

    public EventRsvpService(EventRsvpRepository rsvpRepository, AlumniEventRepository eventRepository,
                            AlumniService alumniService) {
        this.rsvpRepository = rsvpRepository;
        this.eventRepository = eventRepository;
        this.alumniService = alumniService;
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

    public List<EventParticipantResponse> participants(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(NOT_FOUND, "Event not found.");
        }

        List<EventRsvp> joinedRsvps = rsvpRepository.findByEventIdAndStatus(eventId, EventRsvpStatus.JOINED);
        Map<UUID, AlumniProfile> profilesById = findJoinedProfilesById(joinedRsvps);

        return joinedRsvps.stream()
                .filter(rsvp -> profilesById.containsKey(rsvp.profileId()))
                .map(rsvp -> toParticipantResponse(rsvp, profilesById.get(rsvp.profileId())))
                .toList();
    }

    private EventRsvpResponse toResponse(EventRsvp rsvp) {
        List<EventParticipantResponse> participants = participants(rsvp.eventId());
        return new EventRsvpResponse(
                rsvp.eventId(),
                rsvp.profileId(),
                rsvp.status(),
                participants.size(),
                rsvpRepository.countByEventIdAndStatus(rsvp.eventId(), EventRsvpStatus.INTERESTED),
                rsvp.updatedAt(),
                participants
        );
    }

    private Map<UUID, AlumniProfile> findJoinedProfilesById(List<EventRsvp> joinedRsvps) {
        return alumniService.findPublicByIds(joinedRsvps.stream().map(EventRsvp::profileId).toList()).stream()
                .collect(Collectors.toMap(AlumniProfile::id, Function.identity()));
    }

    private EventParticipantResponse toParticipantResponse(EventRsvp rsvp, AlumniProfile profile) {
        return new EventParticipantResponse(
                profile.id(),
                profile.firstName() + " " + profile.lastName(),
                profile.currentTitle(),
                profile.currentCompany(),
                profile.cohortCampus(),
                profile.classYear(),
                profile.avatarUrl(),
                rsvp.updatedAt()
        );
    }
}
