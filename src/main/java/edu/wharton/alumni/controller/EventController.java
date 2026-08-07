package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.EventRequest;
import edu.wharton.alumni.model.AlumniEvent;
import edu.wharton.alumni.model.EventStatus;
import edu.wharton.alumni.security.JwtUser;
import edu.wharton.alumni.service.EventService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<AlumniEvent> listEvents(@RequestParam(required = false, defaultValue = "APPROVED") EventStatus status) {
        return eventService.findByStatus(status);
    }

    @PostMapping
    public AlumniEvent submitEvent(@AuthenticationPrincipal JwtUser user, @Valid @RequestBody EventRequest request) {
        return eventService.submit(request, user.id());
    }
}
