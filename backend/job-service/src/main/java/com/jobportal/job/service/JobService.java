package com.jobportal.job.service;

import com.jobportal.job.cqrs.command.CreateJobCommand;
import com.jobportal.job.cqrs.command.DeleteJobCommand;
import com.jobportal.job.cqrs.command.UpdateJobCommand;
import com.jobportal.job.cqrs.handler.JobCommandHandler;
import com.jobportal.job.cqrs.handler.JobQueryHandler;
import com.jobportal.job.cqrs.query.GetJobByIdQuery;
import com.jobportal.job.cqrs.query.GetRecruiterJobsQuery;
import com.jobportal.job.cqrs.query.SearchJobsQuery;
import com.jobportal.job.dto.JobDto;
import com.jobportal.job.exception.ResourceNotFoundException;
import com.jobportal.job.mapper.JobMapper;
import com.jobportal.job.model.Job;
import com.jobportal.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class JobService {

    private final JobCommandHandler commandHandler;
    private final JobQueryHandler   queryHandler;
    private final JobMapper         jobMapper;
    private final JobRepository     jobRepository;

    // ── Commands ───────────────────────────────────────────────────────────────

    public JobDto.JobResponse createJob(JobDto.CreateJobRequest req, String recruiterId) {
        CreateJobCommand command = jobMapper.toCreateCommand(req);
        command.setRecruiterId(recruiterId);
        return commandHandler.handle(command);
    }

    public JobDto.JobResponse createJob(JobDto.CreateJobRequest req, Long recruiterId) {
        return createJob(req, String.valueOf(recruiterId));
    }

    public JobDto.JobResponse updateJobByReference(String ref, JobDto.UpdateJobRequest req,
                                                    String recruiterId, String role) {
        Long id = resolveId(ref);
        UpdateJobCommand command = jobMapper.toUpdateCommand(req);
        command.setJobId(String.valueOf(id));
        command.setRecruiterId(recruiterId);
        command.setRole(role);
        return commandHandler.handle(command);
    }

    public void deleteJobByReference(String ref, String recruiterId, String role) {
        Long id = resolveId(ref);
        commandHandler.handle(DeleteJobCommand.builder()
            .jobId(String.valueOf(id)).recruiterId(recruiterId).role(role).build());
    }

    public JobDto.JobResponse updateJob(Long jobId, JobDto.UpdateJobRequest req, Long recruiterId, String role) {
        return updateJobByReference(String.valueOf(jobId), req, String.valueOf(recruiterId), role);
    }

    public void deleteJob(Long jobId, Long recruiterId, String role) {
        deleteJobByReference(String.valueOf(jobId), String.valueOf(recruiterId), role);
    }

    public void incrementApplicationCountByReference(String ref) {
        commandHandler.incrementApplicationCount(resolveId(ref));
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    public JobDto.JobResponse getJobByReference(String ref, boolean incrementViewCount) {
        return queryHandler.handle(new GetJobByIdQuery(ref), incrementViewCount);
    }

    public JobDto.JobResponse getJobById(Long jobId) {
        return getJobByReference(String.valueOf(jobId), true);
    }

    // Phase 2: wrapped pagination
    public JobDto.PagedJobResponse searchJobsPaged(JobDto.JobSearchRequest req, int page, int size, String sortBy) {
        SearchJobsQuery q = SearchJobsQuery.builder()
            .keyword(req.getKeyword()).location(req.getLocation()).category(req.getCategory())
            .jobType(req.getJobType()).experienceLevel(req.getExperienceLevel())
            .isRemote(req.getIsRemote()).status(req.getStatus())
            .page(page).size(size).sortBy(sortBy).build();
        return toPagedResponse(queryHandler.handle(q));
    }

    public JobDto.PagedJobResponse getRecruiterJobsPaged(String recruiterId, int page, int size) {
        return toPagedResponse(queryHandler.handle(new GetRecruiterJobsQuery(recruiterId, page, size)));
    }

    public JobDto.JobStatsResponse getStats() { return queryHandler.getStats(); }
    public List<String> getAllCategories()    { return queryHandler.getCategories(); }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Long resolveId(String ref) {
        try { return Long.parseLong(ref); }
        catch (NumberFormatException ignored) {
            return jobRepository.findByUuid(ref)
                .map(Job::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + ref));
        }
    }

    private JobDto.PagedJobResponse toPagedResponse(Page<JobDto.JobResponse> page) {
        return JobDto.PagedJobResponse.builder()
            .content(page.getContent())
            .currentPage(page.getNumber())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .pageSize(page.getSize())
            .first(page.isFirst())
            .last(page.isLast())
            .build();
    }
}
