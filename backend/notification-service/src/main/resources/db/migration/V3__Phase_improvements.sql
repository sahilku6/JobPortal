-- Phase 3: Feedback reviews table
CREATE TABLE IF NOT EXISTS feedback_reviews (
    id           VARCHAR(36)  NOT NULL DEFAULT (UUID()),
    user_id      VARCHAR(36)  NOT NULL,
    user_name    VARCHAR(100) NOT NULL,
    user_role    VARCHAR(50)  NOT NULL,
    review_text  TEXT         NOT NULL,
    rating       TINYINT      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    trigger_type ENUM('FIRST_JOB_POST','FIRST_JOB_APPLY') NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_fb_user    (user_id),
    INDEX idx_fb_rating  (rating),
    INDEX idx_fb_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
