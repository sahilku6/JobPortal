package com.jobportal.notification.kafka;

import com.jobportal.notification.dto.NotificationRequest;
import com.jobportal.notification.event.ApplicationEvent;
import com.jobportal.notification.service.KafkaEventIdempotencyService;
import com.jobportal.notification.service.KafkaEventPayloadParser;
import com.jobportal.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.messaging.handler.annotation.Header;

/**
 * ApplicationEventListener - consumes application-events from Kafka and
 * dispatches email notifications via NotificationService.
 *
 * Events handled:
 *  - SUBMITTED      -> confirmation email to the applicant
 *                    -> new-application email to the recruiter (if present)
 *  - STATUS_CHANGED -> status update email to the applicant
 *  - WITHDRAWN      -> withdrawal confirmation email to the applicant
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventListener {

    private final NotificationService notificationService;
    private final KafkaEventIdempotencyService idempotencyService;
    private final KafkaEventPayloadParser payloadParser;

    @KafkaListener(
        topics  = "${app.kafka.topics.application-events:application-events}",
        groupId = "notification-service"
    )
    public void onApplicationEvent(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        ApplicationEvent event = payloadParser.parse(payload, ApplicationEvent.class);
        String messageKey = topic + ":" + partition + ":" + offset;
        log.info("[KAFKA] Received application event: action={} id={} userId={} status={} applicantEmail={}",
                 event.getAction(), event.getId(), event.getUserId(), event.getStatus(), event.getApplicantEmail());

        try {
            switch (event.getAction()) {
                case "SUBMITTED" -> {
                    log.info("[KAFKA] Processing SUBMITTED event: appId={}", event.getId());
                    sendApplicantSubmitted(event, messageKey);
                    sendRecruiterNewApplication(event, messageKey);
                }
                case "STATUS_CHANGED" -> {
                    log.info("[KAFKA] Processing STATUS_CHANGED event: appId={}", event.getId());
                    sendApplicantStatusChanged(event, messageKey);
                }
                case "WITHDRAWN" -> {
                    log.info("[KAFKA] Processing WITHDRAWN event: appId={}", event.getId());
                    sendApplicantWithdrawn(event, messageKey);
                }
                default -> log.warn("[KAFKA] Unhandled application event action: {}", event.getAction());
            }
        } catch (Exception ex) {
            log.error("[KAFKA] Error processing application event: action={} id={} error={}", 
                     event.getAction(), event.getId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private void sendApplicantSubmitted(ApplicationEvent event, String messageKey) {
        if (event.getApplicantEmail() == null || event.getApplicantEmail().isBlank()) {
            log.warn("Skipping applicant SUBMITTED email for applicationId={} - missing applicantEmail", event.getId());
            return;
        }
        String eventKey = "application:submitted:applicant:" + messageKey;
        // Skip if this exact message was already processed.
        if (!idempotencyService.markIfNew(eventKey)) {
            return;
        }
        try {
            notificationService.sendNotificationSync(
                NotificationRequest.builder()
                    .to(event.getApplicantEmail())
                    .subject("Application received successfully")
                    .body("Your application has been received and is ready for review.")
                    .type("JOB_APPLIED")
                    .userId(event.getUserId())
                    .applicationId(event.getId())
                    .userName(event.getApplicantName())
                    .jobTitle(event.getJobTitle())
                    .companyName(event.getCompanyName())
                    .ctaLabel("View Application")
                    .ctaUrl("/applications")
                    .build()
            );
        } catch (RuntimeException ex) {
            // Undo key so retry can process this event again.
            idempotencyService.unmark(eventKey);
            throw ex;
        }
    }

    private void sendRecruiterNewApplication(ApplicationEvent event, String messageKey) {
        if (event.getRecruiterEmail() == null || event.getRecruiterEmail().isBlank()) {
            log.info("No recruiter email on SUBMITTED event for applicationId={} - recruiter notification skipped", event.getId());
            return;
        }
        String eventKey = "application:submitted:recruiter:" + messageKey;
        if (!idempotencyService.markIfNew(eventKey)) {
            return;
        }
        try {
            notificationService.sendNotificationSync(
                NotificationRequest.builder()
                    .to(event.getRecruiterEmail())
                    .subject("New application for " + defaultValue(event.getJobTitle(), "your job"))
                    .body("A new candidate has applied for "
                        + defaultValue(event.getJobTitle(), "your job")
                        + " at "
                        + defaultValue(event.getCompanyName(), "your company")
                        + ".")
                    .type("NEW_APPLICATION")
                    .userId(event.getRecruiterId())
                    .applicationId(event.getId())
                    .userName(event.getRecruiterName())
                    .jobTitle(event.getJobTitle())
                    .companyName(event.getCompanyName())
                    .status(event.getStatus())
                    .ctaLabel("Review Candidate")
                    .ctaUrl("/recruiter/applications")
                    .build()
            );
        } catch (RuntimeException ex) {
            // Undo key so retry can process this event again.
            idempotencyService.unmark(eventKey);
            throw ex;
        }
    }

    private void sendApplicantStatusChanged(ApplicationEvent event, String messageKey) {
        if (event.getApplicantEmail() == null || event.getApplicantEmail().isBlank()) {
            log.warn("Skipping applicant STATUS_CHANGED email for applicationId={} - missing applicantEmail", event.getId());
            return;
        }
        
        log.info("[STATUS_CHANGED] Processing notification: appId={}, applicantEmail={}, status={}, userId={}",
                 event.getId(), event.getApplicantEmail(), event.getStatus(), event.getUserId());
        
        String eventKey = "application:status-changed:" + messageKey;
        if (!idempotencyService.markIfNew(eventKey)) {
            log.debug("[STATUS_CHANGED] Duplicate event, skipping: eventKey={}", eventKey);
            return;
        }
        try {
            NotificationRequest request = NotificationRequest.builder()
                    .to(event.getApplicantEmail())
                    .subject("Update on your application")
                    .body("Your application status has been updated to: " + defaultValue(event.getStatus(), "UPDATED") + ".")
                    .type("STATUS_CHANGED")
                    .userId(event.getUserId())
                    .applicationId(event.getId())
                    .userName(event.getApplicantName())
                    .jobTitle(event.getJobTitle())
                    .companyName(event.getCompanyName())
                    .status(event.getStatus())
                    .ctaLabel("View Status")
                    .ctaUrl("/applications")
                    .build();
            
            log.info("[STATUS_CHANGED] Sending notification request: to={}, type={}, applicationId={}",
                     request.getTo(), request.getType(), request.getApplicationId());
            
            notificationService.sendNotificationSync(request);
            
            log.info("[STATUS_CHANGED] Notification sent successfully: appId={}", event.getId());
        } catch (RuntimeException ex) {
            log.error("[STATUS_CHANGED] Failed to send notification: appId={}, error={}", event.getId(), ex.getMessage(), ex);
            // Undo key so retry can process this event again.
            idempotencyService.unmark(eventKey);
            throw ex;
        }
    }

    private void sendApplicantWithdrawn(ApplicationEvent event, String messageKey) {
        if (event.getApplicantEmail() == null || event.getApplicantEmail().isBlank()) {
            log.warn("Skipping applicant WITHDRAWN email for applicationId={} - missing applicantEmail", event.getId());
            return;
        }
        String eventKey = "application:withdrawn:" + messageKey;
        if (!idempotencyService.markIfNew(eventKey)) {
            return;
        }
        try {
            notificationService.sendNotificationSync(
                NotificationRequest.builder()
                    .to(event.getApplicantEmail())
                    .subject("Application withdrawn")
                    .body("Your application has been withdrawn successfully. You can apply for other opportunities at any time.")
                    .type("APPLICATION_WITHDRAWN")
                    .userId(event.getUserId())
                    .applicationId(event.getId())
                    .userName(event.getApplicantName())
                    .jobTitle(event.getJobTitle())
                    .companyName(event.getCompanyName())
                    .ctaLabel("Explore Jobs")
                    .ctaUrl("/jobs")
                    .build()
            );
        } catch (RuntimeException ex) {
            // Undo key so retry can process this event again.
            idempotencyService.unmark(eventKey);
            throw ex;
        }
    }

    private String defaultValue(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}

