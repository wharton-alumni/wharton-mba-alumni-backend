package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.AnnouncementRequest;
import edu.wharton.alumni.model.Announcement;
import edu.wharton.alumni.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    public List<Announcement> published() {
        return announcementRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }

    public Announcement create(AnnouncementRequest request) {
        Instant now = Instant.now();
        return announcementRepository.save(new Announcement(UUID.randomUUID(), request.title(), request.body(), request.published(), now, now));
    }

    public Announcement update(UUID id, AnnouncementRequest request) {
        Announcement current = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Announcement not found."));
        return announcementRepository.save(new Announcement(id, request.title(), request.body(), request.published(), current.createdAt(), Instant.now()));
    }

    public void delete(UUID id) {
        announcementRepository.deleteById(id);
    }
}
