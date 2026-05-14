package com.jobportal.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service @RequiredArgsConstructor @Slf4j
public class RedisOtpService {

    private static final String OTP_PREFIX      = "otp:";
    private static final String VERIFIED_PREFIX = "otp:verified:";
    private static final String RATE_PREFIX     = "otp:rate:";
    private static final String RESET_PREFIX    = "pwd_reset:";

    private static final Duration OTP_TTL      = Duration.ofMinutes(10);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(15);
    private static final Duration RATE_TTL     = Duration.ofHours(1);
    private static final int      MAX_OTP_RATE = 5;

    private final RedisTemplate<String, String> redisTemplate;

    // ── Registration OTP ──────────────────────────────────────────────────────

    public void saveOtp(String email, String otp) {
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, OTP_TTL);
    }

    public boolean verifyOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored != null && stored.equals(otp)) {
            redisTemplate.delete(key);
            redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "1", VERIFIED_TTL);
            return true;
        }
        return false;
    }

    public boolean verifyOtpForRegistration(String email, String otp) {
        if (otp == null || otp.isBlank()) return false;
        String key = OTP_PREFIX + email;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored != null && stored.equals(otp)) {
            redisTemplate.delete(key);
            redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "1", VERIFIED_TTL);
            return true;
        }
        return false;
    }

    public boolean isEmailVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(VERIFIED_PREFIX + email));
    }

    public boolean incrementAndCheckRate(String email) {
        String key = RATE_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) redisTemplate.expire(key, RATE_TTL);
        return count != null && count > MAX_OTP_RATE;
    }

    // ── Phase 4: Password reset OTP ───────────────────────────────────────────

    public void savePasswordResetOtp(String email, String otp) {
        redisTemplate.opsForValue().set(RESET_PREFIX + email, otp, OTP_TTL);
        log.debug("Password reset OTP saved for {}", email);
    }

    public boolean peekPasswordResetOtp(String email, String otp) {
        String stored = redisTemplate.opsForValue().get(RESET_PREFIX + email);
        return stored != null && stored.equals(otp);
    }

    public boolean verifyAndDeletePasswordResetOtp(String email, String otp) {
        String key = RESET_PREFIX + email;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored != null && stored.equals(otp)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
