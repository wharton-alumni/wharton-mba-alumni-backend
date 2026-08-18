package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.JobRequest;
import edu.wharton.alumni.model.JobPost;
import edu.wharton.alumni.security.JwtUser;
import edu.wharton.alumni.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public List<JobPost> list() {
        return jobService.findAll();
    }

    @PostMapping
    public JobPost create(@AuthenticationPrincipal JwtUser user, @Valid @RequestBody JobRequest request) {
        return jobService.create(user.id(), request);
    }

    @GetMapping("/{id}")
    public JobPost find(@PathVariable UUID id) {
        return jobService.find(id);
    }

    @PutMapping("/{id}")
    public JobPost update(@AuthenticationPrincipal JwtUser user, @PathVariable UUID id, @Valid @RequestBody JobRequest request) {
        return jobService.update(id, user.id(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal JwtUser user, @PathVariable UUID id) {
        jobService.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }
}
