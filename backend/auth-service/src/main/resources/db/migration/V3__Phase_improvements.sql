-- Phase 1-4: New columns on users table
-- (UUID PK kept as-is since dual-column uuid already exists and is used as reference)

-- Phase 4: Single session JTI support
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS current_token_jti VARCHAR(36) NULL;

-- Phase 4: OAuth provider support
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS oauth_provider_id VARCHAR(255) NULL;

-- Phase 3: Feedback tracking
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS has_posted_first_job BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS has_applied_first_job BOOLEAN NOT NULL DEFAULT FALSE;

-- Phase 4: Forgot password OTP table
CREATE TABLE IF NOT EXISTS password_reset_otps (
    id           VARCHAR(36)  NOT NULL DEFAULT (UUID()),
    email        VARCHAR(100) NOT NULL,
    otp_hash     VARCHAR(255) NOT NULL,
    expires_at   DATETIME     NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_pwo_email   (email),
    INDEX idx_pwo_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
