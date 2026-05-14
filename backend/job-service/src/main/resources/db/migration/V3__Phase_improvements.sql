-- Phase 1: Make applicationDeadline NOT NULL
-- First fill NULLs so NOT NULL constraint doesn't fail
UPDATE jobs SET application_deadline = DATE_ADD(NOW(), INTERVAL 30 DAY)
WHERE application_deadline IS NULL;
ALTER TABLE jobs MODIFY COLUMN application_deadline DATETIME NOT NULL;

-- Phase 1: Index for expiry scheduler
ALTER TABLE jobs ADD INDEX IF NOT EXISTS idx_jobs_deadline_status (application_deadline, status);
