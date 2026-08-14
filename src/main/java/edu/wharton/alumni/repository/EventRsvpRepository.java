package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.EventRsvp;
import edu.wharton.alumni.model.EventRsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRsvpRepository extends JpaRepository<EventRsvp, UUID> {
    Optional<EventRsvp> findByEventIdAndProfileId(UUID eventId, UUID profileId);

    List<EventRsvp> findByProfileId(UUID profileId);

    long countByEventIdAndStatus(UUID eventId, EventRsvpStatus status);
}
