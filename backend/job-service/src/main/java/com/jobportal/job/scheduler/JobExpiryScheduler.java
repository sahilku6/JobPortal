package com.jobportal.job.scheduler;

import com.jobportal.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component @RequiredArgsConstructor @Slf4j
public class JobExpiryScheduler {

    private final JobRepository jobRepository;

    @Scheduled(fixedRate = 3_600_000, initialDelay = 60_000)
    @Transactional
    @CacheEvict(cacheNames = {"job:stats", "job:categories"}, allEntries = true)
    public void expireOverdueJobs() {
        int count = jobRepository.expireOverdueJobs(LocalDateTime.now(),
                com.jobportal.job.model.Job.JobStatus.EXPIRED,
                com.jobportal.job.model.Job.JobStatus.ACTIVE);
        if (count > 0) log.info("[JobExpiryScheduler] Auto-expired {} jobs", count);
    }
}
