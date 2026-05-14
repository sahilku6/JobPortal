package com.jobportal.job.cqrs.handler;

import com.jobportal.job.cqrs.command.CreateJobCommand;
import com.jobportal.job.cqrs.command.DeleteJobCommand;
import com.jobportal.job.cqrs.command.UpdateJobCommand;
import com.jobportal.job.client.ApplicationClient;
import com.jobportal.job.dto.JobDto;
import com.jobportal.job.event.JobEvent;
import com.jobportal.job.exception.ResourceNotFoundException;
import com.jobportal.job.exception.UnauthorizedException;
import com.jobportal.job.kafka.JobEventProducer;
import com.jobportal.job.mapper.JobMapper;
import com.jobportal.job.model.Job;
import com.jobportal.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component @RequiredArgsConstructor @Slf4j
public class JobCommandHandler {

    private final JobRepository     jobRepository;
    private final JobMapper         jobMapper;
    private final ApplicationClient applicationClient;
    private final JobEventProducer  jobEventProducer;

    @Transactional
    @CacheEvict(cacheNames = {"job:stats", "job:categories"}, allEntries = true)
    public JobDto.JobResponse handle(CreateJobCommand command) {
        log.info("CreateJobCommand title={} recruiterId={}", command.getTitle(), command.getRecruiterId());
        // Phase 1: validate deadline
        if (command.getApplicationDeadline() == null)
            throw new IllegalArgumentException("Application deadline is required");
        if (!command.getApplicationDeadline().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Application deadline must be in the future");

        Job job = jobMapper.toEntity(command);
        job.setRecruiterId(Long.parseLong(command.getRecruiterId()));
        Job saved = jobRepository.save(job);
        publishEvent(saved, "CREATED", Collections.emptyList());
        return jobMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"job:stats", "job:categories"}, allEntries = true)
    public JobDto.JobResponse handle(UpdateJobCommand command) {
        Job job = jobRepository.findById(Long.parseLong(command.getJobId()))
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + command.getJobId()));

        if (!job.getRecruiterId().equals(Long.parseLong(command.getRecruiterId()))
                && !"ROLE_ADMIN".equals(command.getRole()))
            throw new UnauthorizedException("Not authorized to update this job");

        if (command.getApplicationDeadline() != null
                && !command.getApplicationDeadline().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Application deadline must be in the future");

        jobMapper.updateEntityFromCommand(command, job);
        Job saved = jobRepository.save(job);
        publishEvent(saved, "UPDATED", Collections.emptyList());
        return jobMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"job:stats", "job:categories"}, allEntries = true)
    public void handle(DeleteJobCommand command) {
        Job job = jobRepository.findById(Long.parseLong(command.getJobId()))
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + command.getJobId()));

        if (!job.getRecruiterId().equals(Long.parseLong(command.getRecruiterId()))
                && !"ROLE_ADMIN".equals(command.getRole()))
            throw new UnauthorizedException("Not authorized to delete this job");

        List<String> emails = Collections.emptyList();
        try { emails = applicationClient.getActiveApplicantEmails(job.getId()); }
        catch (Exception ex) { log.warn("Could not fetch applicant emails: {}", ex.getMessage()); }

        jobRepository.delete(job);
        publishEvent(job, "DELETED", emails);
    }

    @Transactional
    @CacheEvict(cacheNames = {"job:stats"}, allEntries = true)
    public void incrementApplicationCount(Long jobId) {
        jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        jobRepository.incrementApplications(jobId);
    }

    private void publishEvent(Job job, String action, List<String> emails) {
        try {
            jobEventProducer.publish(JobEvent.builder()
                .id(job.getId()).title(job.getTitle()).company(job.getCompany())
                .action(action).applicantEmails(emails).build());
        } catch (Exception ex) {
            log.warn("Kafka publish failed for job {} id={}: {}", action, job.getId(), ex.getMessage());
        }
    }
}
