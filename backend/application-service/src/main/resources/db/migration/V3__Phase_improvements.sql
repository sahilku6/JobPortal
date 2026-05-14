-- Phase 1: Make resume_url NOT NULL
UPDATE job_applications SET resume_url = 'legacy-missing-resume'
WHERE resume_url IS NULL OR resume_url = '';
ALTER TABLE job_applications MODIFY COLUMN resume_url VARCHAR(500) NOT NULL;
