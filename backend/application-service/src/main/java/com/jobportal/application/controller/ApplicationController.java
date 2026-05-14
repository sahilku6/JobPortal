package com.jobportal.application.controller;

import com.jobportal.application.dto.ApplicationDto;
import com.jobportal.application.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController @RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications")
@SecurityRequirement(name = "Bearer Auth")
public class ApplicationController {

    private final ApplicationService applicationService;

    // Phase 1: resumeUrl required in body
    @PostMapping
    @Operation(summary = "Apply for a job (resumeUrl required in body)")
    public ResponseEntity<ApplicationDto.ApplicationResponse> apply(
            @Valid @RequestBody ApplicationDto.ApplyRequest req,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.apply(req, userId));
    }

    // Resume multipart — required field
    @PostMapping(value = "/with-resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Apply with resume upload (required)")
    public ResponseEntity<ApplicationDto.ApplicationResponse> applyWithResume(
            @RequestParam("jobId") String jobId,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestParam("resume") MultipartFile resume,
            @RequestHeader("X-User-Id") Long userId) throws IOException {
        if (resume == null || resume.isEmpty())
            throw new IllegalArgumentException("Resume is required to apply for this job");
        // Validate file type
        String filename = resume.getOriginalFilename() != null ? resume.getOriginalFilename().toLowerCase() : "";
        if (!filename.endsWith(".pdf") && !filename.endsWith(".doc") && !filename.endsWith(".docx"))
            throw new IllegalArgumentException("Resume must be a PDF, DOC, or DOCX file");
        ApplicationDto.ApplyRequest req = ApplicationDto.ApplyRequest.builder()
            .jobId(jobId).coverLetter(coverLetter).build();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(applicationService.uploadResumeAndApply(req, userId, resume));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my applications (Job Seeker)")
    public ResponseEntity<Page<ApplicationDto.ApplicationResponse>> getMyApplications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(applicationService.getUserApplications(userId, page, size));
    }

    @GetMapping("/my/stats")
    public ResponseEntity<ApplicationDto.ApplicationStatsResponse> getMyStats(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(applicationService.getUserStats(userId));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<Page<ApplicationDto.ApplicationResponse>> getJobApplicants(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(applicationService.getJobApplicantsByReference(jobId, page, size));
    }

    @GetMapping("/recruiter/inbox")
    public ResponseEntity<Page<ApplicationDto.ApplicationResponse>> getRecruiterApplications(
            @RequestHeader("X-User-Id") Long recruiterId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(applicationService.getRecruiterApplications(recruiterId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDto.ApplicationResponse> getById(
            @PathVariable String id,
            @RequestHeader("X-User-Id")   Long userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(applicationService.getApplicationByReference(id, userId, role));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationDto.ApplicationResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody ApplicationDto.UpdateStatusRequest req,
            @RequestHeader("X-User-Id")   Long recruiterId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(applicationService.updateStatusByReference(id, req, recruiterId, role));
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<Void> withdraw(
            @PathVariable String id,
            @RequestHeader("X-User-Id") Long userId) {
        applicationService.withdrawApplicationByReference(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/all")
    public ResponseEntity<Page<ApplicationDto.ApplicationResponse>> getAllApplications(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(applicationService.getAllApplications(page, size, keyword, status));
    }

    @GetMapping("/internal/jobs/{jobId}/active-applicant-emails")
    public ResponseEntity<List<String>> getActiveApplicantEmails(@PathVariable String jobId) {
        return ResponseEntity.ok(applicationService.getActiveApplicantEmailsByJobReference(jobId));
    }
}
