package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.AnnouncementRequest;
import edu.wharton.alumni.model.Announcement;
import edu.wharton.alumni.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/api/announcements")
    public List<Announcement> published() {
        return announcementService.published();
    }

    @PostMapping("/api/admin/announcements")
    public Announcement create(@Valid @RequestBody AnnouncementRequest request) {
        return announcementService.create(request);
    }

    @PatchMapping("/api/admin/announcements/{id}")
    public Announcement update(@PathVariable UUID id, @Valid @RequestBody AnnouncementRequest request) {
        return announcementService.update(id, request);
    }

    @DeleteMapping("/api/admin/announcements/{id}")
    public void delete(@PathVariable UUID id) {
        announcementService.delete(id);
    }
}
