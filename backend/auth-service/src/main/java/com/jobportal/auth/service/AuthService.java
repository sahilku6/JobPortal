package com.jobportal.auth.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jobportal.auth.client.NotificationClient;
import com.jobportal.auth.dto.AuthDto;
import com.jobportal.auth.dto.NotificationRequest;
import com.jobportal.auth.event.UserEvent;
import com.jobportal.auth.exception.*;
import com.jobportal.auth.kafka.UserEventProducer;
import com.jobportal.auth.mapper.UserMapper;
import com.jobportal.auth.model.User;
import com.jobportal.auth.repository.UserRepository;
import com.jobportal.auth.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class AuthService {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtUtil            jwtUtil;
    private final UserMapper         userMapper;
    private final Cloudinary         cloudinary;
    private final NotificationClient notificationClient;
    private final RedisOtpService    redisOtpService;
    private final UserEventProducer  userEventProducer;

    @Value("${otp.expiry-minutes:10}")
    private long otpExpiryMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    // ── Email OTP ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthDto.MessageResponse requestEmailOtp(AuthDto.SendOtpRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email))
            throw new DuplicateResourceException("Email already registered");
        if (redisOtpService.incrementAndCheckRate(email))
            throw new BadRequestException("Too many OTP requests. Please try again later.");
        String otp = generateOtp();
        redisOtpService.saveOtp(email, otp);
        sendEmail(email, "Verify your CareerBridge email",
            "Your verification code is " + otp + ". It expires in " + otpExpiryMinutes + " minutes.",
            "EMAIL_VERIFICATION", "/login");
        return AuthDto.MessageResponse.builder().message("OTP sent to your email").build();
    }

    @Transactional
    public AuthDto.MessageResponse verifyEmailOtp(AuthDto.VerifyOtpRequest req) {
        boolean ok = redisOtpService.verifyOtp(req.getEmail().trim().toLowerCase(), req.getOtp());
        if (!ok) throw new BadRequestException("Invalid or expired OTP");
        return AuthDto.MessageResponse.builder().message("Email verified successfully").build();
    }

    // ── Register / Login ───────────────────────────────────────────────────────

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email))
            throw new DuplicateResourceException("Email already registered");
        if (userRepository.existsByUsername(req.getUsername()))
            throw new DuplicateResourceException("Username already taken");
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank()
                && userRepository.existsByPhoneNumber(req.getPhoneNumber().trim()))
            throw new DuplicateResourceException("Phone number already registered");

        boolean emailVerified = redisOtpService.isEmailVerified(email);
        if (!emailVerified)
            emailVerified = redisOtpService.verifyOtpForRegistration(email, req.getOtp());
        if (!emailVerified)
            throw new BadRequestException("Email not verified. Please verify your email first.");

        User user = userMapper.toEntity(req);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setIsEmailVerified(true);
        User saved = userRepository.save(user);
        publishEvent(saved, "REGISTERED");
        return buildAuthResponse(saved);
    }

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new UnauthorizedException("Invalid credentials");
        if (!Boolean.TRUE.equals(user.getIsActive()))
            throw new UnauthorizedException("Account is deactivated");
        if (!Boolean.TRUE.equals(user.getIsEmailVerified()))
            throw new UnauthorizedException("Please verify your email before login");

        // Phase 4: rotate JTI on every login → invalidates previous sessions
        String jti = UUID.randomUUID().toString();
        user.setCurrentTokenJti(jti);
        userRepository.save(user);
        return buildAuthResponse(user, jti);
    }

    public AuthDto.AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken))
            throw new UnauthorizedException("Invalid or expired refresh token");
        Long userId = jwtUtil.extractUserId(refreshToken);
        String refreshJti = jwtUtil.extractJti(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String currentJti = user.getCurrentTokenJti();
        if (currentJti == null || refreshJti == null || !currentJti.equals(refreshJti)) {
            throw new UnauthorizedException("Session invalidated. Please log in again.");
        }

        return buildAuthResponse(user);
    }

    // ── Phase 4: Forgot Password ───────────────────────────────────────────────

    @Transactional
    public AuthDto.MessageResponse requestPasswordResetOtp(AuthDto.ForgotPasswordRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            String otp = generateOtp();
            redisOtpService.savePasswordResetOtp(email, otp);
            sendEmail(email, "Reset your CareerBridge password",
                "Your password reset code is " + otp + ". It expires in " + otpExpiryMinutes + " minutes.",
                "PASSWORD_RESET", "/forgot-password");
        }
        // Always return same message (don't reveal if email exists)
        return AuthDto.MessageResponse.builder()
            .message("If this email is registered, a reset code has been sent.").build();
    }

    public AuthDto.MessageResponse verifyPasswordResetOtp(AuthDto.VerifyResetOtpRequest req) {
        boolean ok = redisOtpService.peekPasswordResetOtp(req.getEmail().trim().toLowerCase(), req.getOtp());
        if (!ok) throw new BadRequestException("Invalid or expired OTP");
        return AuthDto.MessageResponse.builder().message("OTP verified").build();
    }

    @Transactional
    public AuthDto.MessageResponse resetPassword(AuthDto.ResetPasswordRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        boolean valid = redisOtpService.verifyAndDeletePasswordResetOtp(email, req.getOtp());
        if (!valid) throw new BadRequestException("Invalid or expired OTP");
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setCurrentTokenJti(null); // invalidate all sessions
        userRepository.save(user);
        return AuthDto.MessageResponse.builder().message("Password reset successfully. Please log in again.").build();
    }

    // ── Phase 4: OAuth ─────────────────────────────────────────────────────────

    @Transactional
    public AuthDto.AuthResponse processOAuthLogin(String provider, String providerId, String email, String name) {
        User user = userRepository.findByOauthProviderAndOauthProviderId(provider, providerId)
            .or(() -> email != null ? userRepository.findByEmail(email.toLowerCase()) : java.util.Optional.empty())
            .orElseGet(() -> User.builder()
                .email(email != null ? email.toLowerCase() : provider + "_" + providerId + "@oauth.local")
                .username(generateUniqueUsername(name, provider, providerId))
                .fullName(name != null ? name : provider + " User")
                .password("")
                .role(User.Role.ROLE_JOB_SEEKER)
                .oauthProvider(provider)
                .oauthProviderId(providerId)
                .isEmailVerified(true)
                .isActive(true)
                .build());
        user.setOauthProvider(provider);
        user.setOauthProviderId(providerId);
        String jti = UUID.randomUUID().toString();
        user.setCurrentTokenJti(jti);
        User saved = userRepository.save(user);
        return buildAuthResponse(saved, jti);
    }

    // ── Phase 3: Feedback gating ───────────────────────────────────────────────

    @Transactional
    public AuthDto.FeedbackPromptResponse shouldPromptFeedback(Long userId, String triggerType) {
        boolean prompt = false;
        if ("FIRST_JOB_POST".equals(triggerType)) {
            prompt = userRepository.markFirstJobPosted(userId) > 0;
        } else if ("FIRST_JOB_APPLY".equals(triggerType)) {
            prompt = userRepository.markFirstJobApplied(userId) > 0;
        }
        return AuthDto.FeedbackPromptResponse.builder()
            .shouldPrompt(prompt).triggerType(triggerType).build();
    }

    // ── Profile ────────────────────────────────────────────────────────────────

    public AuthDto.UserResponse getCurrentUser(Long userId) {
        return userMapper.toUserResponse(userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @Transactional
    public AuthDto.UserResponse updateProfile(Long userId, AuthDto.UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (req.getFullName()    != null && !req.getFullName().isBlank())    user.setFullName(req.getFullName().trim());
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank()) user.setPhoneNumber(req.getPhoneNumber().trim());
        if (req.getBio()         != null) user.setBio(req.getBio());
        if (req.getLocation()    != null) user.setLocation(req.getLocation());
        if (req.getCompanyName() != null) user.setCompanyName(req.getCompanyName());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, AuthDto.ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new BadRequestException("Current password is incorrect");
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setCurrentTokenJti(null);
        userRepository.save(user);
    }

    @Transactional
    public AuthDto.UserResponse uploadProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        validateImageFile(file);
        try {
            if (user.getProfileImagePublicId() != null) {
                cloudinary.uploader().destroy(user.getProfileImagePublicId(), ObjectUtils.asMap("invalidate", true, "resource_type", "image"));
            }
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", "job-portal/profiles/" + userId,
                    "resource_type", "image",
                    "invalidate", true,
                    "transformation", ObjectUtils.asMap(
                        "width", 400, "height", 400,
                        "crop", "fill", "gravity", "face", "quality", "auto"
                    )
                ));
            user.setProfileImageUrl((String) result.get("secure_url"));
            user.setProfileImagePublicId((String) result.get("public_id"));
            return userMapper.toUserResponse(userRepository.save(user));
        } catch (IOException e) {
            log.error("Profile image upload failed for user {}: {}", userId, e.getMessage());
            throw new BadRequestException("Failed to upload profile image");
        }
    }

    private static final java.util.List<String> ALLOWED_IMAGE_TYPES = java.util.Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp");

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("File cannot be empty");
        if (file.getSize() > 5 * 1024 * 1024) throw new BadRequestException("File must not exceed 5MB");
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct)) throw new BadRequestException("Invalid file type. Allowed: image (JPEG/PNG/WebP)");
    }

    // ── Admin ──────────────────────────────────────────────────────────────────

    public Page<AuthDto.UserResponse> getAllUsers(Pageable pageable, String keyword, User.Role role, Boolean isActive) {
        return userRepository.searchUsers(keyword, role, isActive, pageable).map(userMapper::toUserResponse);
    }

    public Page<AuthDto.UserResponse> getUsersByRole(User.Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(userMapper::toUserResponse);
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepository.save(user);
        publishEvent(user, "TOGGLED_STATUS");
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String email = user.getEmail();
        userRepository.deleteById(userId);
        publishEvent(User.builder().id(userId).email(email).build(), "DELETED");
    }

    public AuthDto.UserResponse getCurrentUserByReference(String ref) {
        return userMapper.toUserResponse(resolveUser(ref));
    }

    @Transactional
    public void toggleUserStatusByReference(String ref) { toggleUserStatus(resolveUser(ref).getId()); }

    @Transactional
    public void deleteUserByReference(String ref) { deleteUser(resolveUser(ref).getId()); }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private User resolveUser(String ref) {
        if (ref == null || ref.isBlank()) throw new ResourceNotFoundException("User not found");
        try { return userRepository.findById(Long.parseLong(ref))
                .orElseThrow(() -> new ResourceNotFoundException("User not found")); }
        catch (NumberFormatException ignored) {
            return userRepository.findByUuid(ref)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
    }

    private AuthDto.AuthResponse buildAuthResponse(User user) {
        String jti = user.getCurrentTokenJti() != null ? user.getCurrentTokenJti() : UUID.randomUUID().toString();
        return buildAuthResponse(user, jti);
    }

    private AuthDto.AuthResponse buildAuthResponse(User user, String jti) {
        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(), jti);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), jti);
        return AuthDto.AuthResponse.builder()
            .accessToken(accessToken).refreshToken(refreshToken)
            .tokenType("Bearer").expiresIn(jwtUtil.getExpiration())
            .user(userMapper.toUserResponse(user)).build();
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void sendEmail(String to, String subject, String body, String type, String ctaUrl) {
        try {
            notificationClient.sendNotification(NotificationRequest.builder()
                .to(to).subject(subject).body(body).type(type)
                .ctaLabel("Proceed").ctaUrl(ctaUrl).build());
        } catch (Exception ex) {
            log.warn("Email dispatch failed for {}: {}", to, ex.getMessage());
        }
    }

    private void publishEvent(User user, String action) {
        try {
            userEventProducer.publish(UserEvent.builder()
                .id(user.getId()).email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .action(action).build());
        } catch (Exception ex) {
            log.warn("Kafka publish failed: {}", ex.getMessage());
        }
    }

    private String generateUniqueUsername(String name, String provider, String providerId) {
        String base = (name != null ? name.replaceAll("\\s+", "").toLowerCase() : provider)
            .replaceAll("[^a-z0-9]", "");
        if (base.isBlank()) base = provider;
        String candidate = base;
        int attempt = 0;
        while (userRepository.existsByUsername(candidate)) candidate = base + (++attempt);
        return candidate;
    }
}
