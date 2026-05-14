package com.jobportal.auth.controller;

import com.jobportal.auth.dto.AuthDto;
import com.jobportal.auth.model.User;
import com.jobportal.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController @RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth Service APIs")
public class AuthController {

    private final AuthService authService;

    // ── Public ─────────────────────────────────────────────────────────────────
    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/api/v1/auth/request-email-otp")
    public ResponseEntity<AuthDto.MessageResponse> requestEmailOtp(@Valid @RequestBody AuthDto.SendOtpRequest req) {
        return ResponseEntity.ok(authService.requestEmailOtp(req));
    }

    @PostMapping("/api/v1/auth/verify-email-otp")
    public ResponseEntity<AuthDto.MessageResponse> verifyEmailOtp(@Valid @RequestBody AuthDto.VerifyOtpRequest req) {
        return ResponseEntity.ok(authService.verifyEmailOtp(req));
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/api/v1/auth/refresh")
    public ResponseEntity<AuthDto.AuthResponse> refresh(@Valid @RequestBody AuthDto.RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refreshToken(req.getRefreshToken()));
    }

    // ── Phase 4: Forgot password ───────────────────────────────────────────────
    @PostMapping("/api/v1/auth/forgot-password/request")
    @Operation(summary = "Step 1 - Request password reset OTP")
    public ResponseEntity<AuthDto.MessageResponse> forgotPasswordRequest(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.requestPasswordResetOtp(req));
    }

    @PostMapping("/api/v1/auth/forgot-password/verify")
    @Operation(summary = "Step 2 - Verify reset OTP")
    public ResponseEntity<AuthDto.MessageResponse> forgotPasswordVerify(
            @Valid @RequestBody AuthDto.VerifyResetOtpRequest req) {
        return ResponseEntity.ok(authService.verifyPasswordResetOtp(req));
    }

    @PostMapping("/api/v1/auth/forgot-password/reset")
    @Operation(summary = "Step 3 - Set new password")
    public ResponseEntity<AuthDto.MessageResponse> forgotPasswordReset(
            @Valid @RequestBody AuthDto.ResetPasswordRequest req) {
        return ResponseEntity.ok(authService.resetPassword(req));
    }

    // ── Protected ───────────────────────────────────────────────────────────────
    @GetMapping("/api/v1/users/me")
    @Operation(security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<AuthDto.UserResponse> getMe(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }

    @PutMapping("/api/v1/users/me")
    public ResponseEntity<AuthDto.UserResponse> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AuthDto.UpdateProfileRequest req) {
        return ResponseEntity.ok(authService.updateProfile(userId, req));
    }

    @PostMapping("/api/v1/users/me/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AuthDto.ChangePasswordRequest req) {
        authService.changePassword(userId, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/users/me/profile-image")
    public ResponseEntity<AuthDto.UserResponse> uploadProfileImage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(authService.uploadProfileImage(userId, file));
    }

    @GetMapping("/api/v1/users/{id}")
    public ResponseEntity<AuthDto.UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(authService.getCurrentUserByReference(id));
    }

    // ── Phase 3: Feedback prompt ───────────────────────────────────────────────
    @GetMapping("/api/v1/users/should-prompt-feedback")
    @Operation(security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<AuthDto.FeedbackPromptResponse> shouldPromptFeedback(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String triggerType) {
        return ResponseEntity.ok(authService.shouldPromptFeedback(userId, triggerType));
    }

    // ── Admin ───────────────────────────────────────────────────────────────────
    @GetMapping("/api/v1/users")
    public ResponseEntity<Page<AuthDto.UserResponse>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(authService.getAllUsers(pageable, normalize(keyword), User.Role.fromValue(role), isActive));
    }

    @GetMapping("/api/v1/users/role/{role}")
    public ResponseEntity<Page<AuthDto.UserResponse>> getUsersByRole(
            @PathVariable User.Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(authService.getUsersByRole(role, PageRequest.of(page, size)));
    }

    @PatchMapping("/api/v1/users/{id}/toggle-status")
    public ResponseEntity<Void> toggleStatus(@PathVariable String id) {
        authService.toggleUserStatusByReference(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        authService.deleteUserByReference(id);
        return ResponseEntity.noContent().build();
    }

    private String normalize(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
