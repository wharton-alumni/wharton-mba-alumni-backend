package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.EventStatusRequest;
import edu.wharton.alumni.model.AlumniEvent;
import edu.wharton.alumni.service.EventService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {
    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<AlumniEvent> listAll() {
        return eventService.findByStatus(null);
    }

    @PatchMapping("/{id}/status")
    public AlumniEvent updateStatus(@PathVariable UUID id, @Valid @RequestBody EventStatusRequest request) {
        return eventService.updateStatus(id, request.status());
    }
}
