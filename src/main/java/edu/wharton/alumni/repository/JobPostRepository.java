package edu.wharton.alumni.repository;

import edu.wharton.alumni.model.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPostRepository extends JpaRepository<JobPost, UUID> {
    List<JobPost> findAllByOrderByCreatedAtDesc();
}
