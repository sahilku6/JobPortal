package com.jobportal.notification.controller;

import com.jobportal.notification.dto.NotificationRequest;
import com.jobportal.notification.model.FeedbackReview;
import com.jobportal.notification.model.Notification;
import com.jobportal.notification.security.jwt.JwtUtil;
import com.jobportal.notification.service.FeedbackService;
import com.jobportal.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final FeedbackService     feedbackService;
    private final JwtUtil             jwtUtil;

    @PostMapping("/send")
    @Operation(summary = "Send notification (internal)")
    public ResponseEntity<Void> send(@RequestBody NotificationRequest req) {
        notificationService.sendNotification(req);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/my")
    @Operation(security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<Page<Notification>> getMy(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, page, size));
    }

    /**
     * SSE streaming endpoint.
     *
     * The gateway's JwtAuthFilter validates the token and injects X-User-Id for normal requests.
     * However, EventSource (SSE) clients cannot set custom headers, so we also support
     * resolving userId from a ?token= query parameter.
     *
     * Resolution order:
     *  1. X-User-Id header (injected by gateway — preferred)
     *  2. ?token= query param (decoded locally using JwtUtil)
     */
    @GetMapping(value = "/my/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<SseEmitter> streamMyNotifications(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value  = "token",      required = false) String token) {

        Long userId = headerUserId;

        // Fallback: decode token from query param when gateway header is absent
        if (userId == null && token != null && !token.isBlank()) {
            userId = jwtUtil.extractUserId(token);
        }

        if (userId == null) {
            log.warn("SSE subscribe rejected: no valid userId resolved");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("SSE subscribe userId={}", userId);
        return ResponseEntity.ok(notificationService.subscribe(userId));
    }

    @PatchMapping("/my/{notificationId}/read")
    @Operation(security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String notificationId) {
        return notificationService.markAsReadByReference(userId, notificationId)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    @PatchMapping("/my/read-all")
    @Operation(security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<Void> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/my")
    @Operation(security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<Void> clearMy(@RequestHeader("X-User-Id") Long userId) {
        notificationService.clearUserNotifications(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/my/{notificationId}")
    @Operation(security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<Void> deleteOne(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String notificationId) {
        return notificationService.deleteUserNotificationByReference(userId, notificationId)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    @PostMapping("/feedback")
    @Operation(security = @SecurityRequirement(name = "Bearer Auth"))
    public ResponseEntity<Void> submitFeedback(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody FeedbackRequest req) {
        feedbackService.submit(userId, req.getReviewText(), req.getRating(),
            req.getTriggerType(), req.getUserName(), req.getUserRole());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/feedback/public")
    public ResponseEntity<List<FeedbackReview>> getPublicFeedback(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(feedbackService.getPublicReviews(limit));
    }

    @Data
    public static class FeedbackRequest {
        private String  reviewText;
        private Integer rating;
        private String  triggerType;
        private String  userName;
        private String  userRole;
    }
}
