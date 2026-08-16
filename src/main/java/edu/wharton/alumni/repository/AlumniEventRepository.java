package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.AlumniEvent;
import edu.wharton.alumni.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AlumniEventRepository extends JpaRepository<AlumniEvent, UUID> {
    List<AlumniEvent> findByStatusOrderByCreatedAtDesc(EventStatus status);

    List<AlumniEvent> findAllByOrderByCreatedAtDesc();

    void deleteByExternalManagedTrueAndIdNotIn(Collection<UUID> ids);
}
