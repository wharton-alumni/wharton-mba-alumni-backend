package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.EventRequest;
import edu.wharton.alumni.dto.EventRsvpRequest;
import edu.wharton.alumni.dto.EventRsvpResponse;
import edu.wharton.alumni.model.AlumniEvent;
import edu.wharton.alumni.model.EventStatus;
import edu.wharton.alumni.security.JwtUser;
import edu.wharton.alumni.service.EventRsvpService;
import edu.wharton.alumni.service.EventService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;
    private final EventRsvpService rsvpService;

    public EventController(EventService eventService, EventRsvpService rsvpService) {
        this.eventService = eventService;
        this.rsvpService = rsvpService;
    }

    @GetMapping
    public List<AlumniEvent> listEvents(@RequestParam(required = false, defaultValue = "APPROVED") EventStatus status) {
        return eventService.findByStatus(status);
    }

    @PostMapping
    public AlumniEvent submitEvent(@AuthenticationPrincipal JwtUser user, @Valid @RequestBody EventRequest request) {
        return eventService.submit(request, user.id());
    }

    @GetMapping("/rsvps/me")
    public List<EventRsvpResponse> myRsvps(@AuthenticationPrincipal JwtUser user) {
        return rsvpService.findForProfile(user.id());
    }

    @PutMapping("/{eventId}/rsvp")
    public EventRsvpResponse updateRsvp(@AuthenticationPrincipal JwtUser user,
                                        @PathVariable UUID eventId,
                                        @Valid @RequestBody EventRsvpRequest request) {
        return rsvpService.update(eventId, user.id(), request.status());
    }
}
