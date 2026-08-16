package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.ExternalEventSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalEventSyncStateRepository extends JpaRepository<ExternalEventSyncState, String> {
}
