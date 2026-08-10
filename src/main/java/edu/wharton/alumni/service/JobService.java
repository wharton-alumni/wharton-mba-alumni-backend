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
        return jobPostRepository.findAllByOrderByCreatedAtDesc();
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
                Instant.now()
        ));
    }
}
