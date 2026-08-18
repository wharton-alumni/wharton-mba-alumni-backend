package edu.wharton.alumni.service;

import edu.wharton.alumni.dto.JobRequest;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.model.JobPost;
import edu.wharton.alumni.repository.JobPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class JobService {
    private final JobPostRepository jobPostRepository;
    private final AlumniService alumniService;

    public JobService(JobPostRepository jobPostRepository, AlumniService alumniService) {
        this.jobPostRepository = jobPostRepository;
        this.alumniService = alumniService;
    }

    public List<JobPost> findAll() {
        return jobPostRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(job -> job.endDate() == null || job.endDate().isAfter(Instant.now()))
                .toList();
    }

    public JobPost find(UUID id) {
        return jobPostRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Job post not found."));
    }

    public JobPost create(UUID profileId, JobRequest request) {
        AlumniProfile poster = alumniService.findInternal(profileId);
        return jobPostRepository.save(new JobPost(
                UUID.randomUUID(),
                request.title(),
                request.company(),
                request.location(),
                request.externalLink(),
                request.applicationLink(),
                request.description(),
                poster.id(),
                poster.firstName() + " " + poster.lastName(),
                Instant.now(),
                request.startDate(),
                request.endDate()
        ));
    }

    public JobPost update(UUID id, UUID profileId, JobRequest request) {
        JobPost existing = find(id);
        if (!existing.postedById().equals(profileId)) {
            throw new ResponseStatusException(FORBIDDEN, "You can only edit your own job posts.");
        }
        return jobPostRepository.save(new JobPost(
                existing.id(),
                request.title(),
                request.company(),
                request.location(),
                request.externalLink(),
                request.applicationLink(),
                request.description(),
                existing.postedById(),
                existing.postedByName(),
                existing.createdAt(),
                request.startDate(),
                request.endDate()
        ));
    }

    public void delete(UUID id, UUID profileId) {
        JobPost existing = find(id);
        if (!existing.postedById().equals(profileId)) {
            throw new ResponseStatusException(FORBIDDEN, "You can only delete your own job posts.");
        }
        jobPostRepository.deleteById(id);
    }
}
