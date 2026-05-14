package com.jobportal.job.controller;

import com.jobportal.job.dto.JobDto;
import com.jobportal.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job management APIs")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create job posting", security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<JobDto.JobResponse> createJob(
            @Valid @RequestBody JobDto.CreateJobRequest req,
            @RequestHeader("X-User-Id")   String recruiterId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ROLE_RECRUITER") && !role.equals("ROLE_ADMIN"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(req, recruiterId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update job posting", security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<JobDto.JobResponse> updateJob(
            @PathVariable String id,
            @Valid @RequestBody JobDto.UpdateJobRequest req,
            @RequestHeader("X-User-Id")   String recruiterId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ROLE_RECRUITER") && !role.equals("ROLE_ADMIN"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(jobService.updateJobByReference(id, req, recruiterId, role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete job posting", security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<Void> deleteJob(
            @PathVariable String id,
            @RequestHeader("X-User-Id")   String recruiterId,
            @RequestHeader("X-User-Role") String role) {
        jobService.deleteJobByReference(id, recruiterId, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<JobDto.JobResponse> getJobById(
            @PathVariable String id,
            @RequestParam(defaultValue = "true") boolean incrementViewCount) {
        return ResponseEntity.ok(jobService.getJobByReference(id, incrementViewCount));
    }

    // Phase 2: returns PagedJobResponse
    @GetMapping("/search")
    @Operation(summary = "Search jobs")
    public ResponseEntity<JobDto.PagedJobResponse> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) Boolean isRemote,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        JobDto.JobSearchRequest req = JobDto.JobSearchRequest.builder()
            .keyword(keyword).location(location).category(category)
            .jobType(jobType).experienceLevel(experienceLevel).isRemote(isRemote)
            .status(normalizeStatus(status)).build();
        return ResponseEntity.ok(jobService.searchJobsPaged(req, page, size, sortBy));
    }

    @GetMapping
    @Operation(summary = "Get all active jobs")
    public ResponseEntity<JobDto.PagedJobResponse> getAllJobs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobService.searchJobsPaged(
            JobDto.JobSearchRequest.builder().status("ACTIVE").build(), page, size, "createdAt"));
    }

    @GetMapping("/recruiter/my-jobs")
    @Operation(summary = "Get my jobs (Recruiter)", security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<JobDto.PagedJobResponse> getRecruiterJobs(
            @RequestHeader("X-User-Id") String recruiterId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobService.getRecruiterJobsPaged(recruiterId, page, size));
    }

    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<JobDto.PagedJobResponse> getJobsByRecruiter(
            @PathVariable String recruiterId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobService.getRecruiterJobsPaged(recruiterId, page, size));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(jobService.getAllCategories());
    }

    @GetMapping("/stats")
    public ResponseEntity<JobDto.JobStatsResponse> getStats() {
        return ResponseEntity.ok(jobService.getStats());
    }

    @PatchMapping("/{id}/increment-application")
    public ResponseEntity<Void> incrementApplication(@PathVariable String id) {
        jobService.incrementApplicationCountByReference(id);
        return ResponseEntity.noContent().build();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "ACTIVE";
        if ("ALL".equalsIgnoreCase(status.trim())) return null;
        return status.trim();
    }
}
