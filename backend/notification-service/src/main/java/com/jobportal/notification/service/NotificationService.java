package com.jobportal.notification.service;

import com.jobportal.notification.dto.NotificationRequest;
import com.jobportal.notification.model.Notification;
import com.jobportal.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor @Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final NotificationStreamService notificationStreamService;

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Async
    public void sendNotification(NotificationRequest req) {
        // Used by HTTP/internal calls. Failures are logged and persisted only.
        sendNotificationInternal(req, false);
    }

    @Transactional
    public void sendNotificationSync(NotificationRequest req) {
        // Used by Kafka listeners. Throw on failure so retry/DLT can work.
        sendNotificationInternal(req, true);
    }

    private void sendNotificationInternal(NotificationRequest req, boolean throwOnFailure) {
        log.info("[NOTIFICATION] Processing notification: type={}, to={}, userId={}, applicationId={}, throwOnFailure={}",
                 req.getType(), req.getTo(), req.getUserId(), req.getApplicationId(), throwOnFailure);
        
        // Guard: 'to' may be absent when the request originates from a Kafka event
        // that carries only userId (no email). Persist the record for auditing but skip SMTP.
        if (req.getTo() == null || req.getTo().isBlank()) {
            log.warn("[NOTIFICATION] sendNotification called without recipient email - persisting record only. type={} userId={}",
                     req.getType(), req.getUserId());
                Notification notification = notificationRepository.save(Notification.builder()
                    .subject(req.getSubject()).body(req.getBody())
                    .type(req.getType()).userId(req.getUserId()).applicationId(req.getApplicationId())
                    .status(Notification.NotificationStatus.FAILED)
                    .errorMessage("No recipient email address provided")
                    .build());
                if (notification.getUserId() != null && notification.getId() != null) {
                    log.info("[NOTIFICATION] Publishing SSE event (no email): userId={}, notificationId={}", 
                             notification.getUserId(), notification.getId());
                    notificationStreamService.publishNotification(notification.getUserId(), notification.getId());
                }
                if (throwOnFailure) {
                    throw new IllegalArgumentException("No recipient email address provided");
                }
                return;
        }

        Notification notification = Notification.builder()
            .to(req.getTo()).subject(req.getSubject()).body(req.getBody())
            .type(req.getType()).userId(req.getUserId()).applicationId(req.getApplicationId())
            .build();
        notification = notificationRepository.save(notification);
        
        log.info("[NOTIFICATION] Saved notification record: id={}, to={}, type={}", 
                 notification.getId(), notification.getTo(), notification.getType());
        
        try {
            log.info("[NOTIFICATION] Sending email: to={}, subject={}, type={}", 
                     req.getTo(), req.getSubject(), req.getType());
            
            sendHtmlEmail(req.getTo(), req.getSubject(), buildHtml(req));
            
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            
            log.info("[NOTIFICATION] Email sent successfully: to={}, type={}, notificationId={}", 
                     req.getTo(), req.getType(), notification.getId());
        } catch (Exception e) {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            log.error("[NOTIFICATION] Email send failed: to={}, error={}", req.getTo(), e.getMessage(), e);
            
            if (throwOnFailure) {
                // Re-throw for Kafka listener path.
                notification = notificationRepository.save(notification);
                if (notification.getUserId() != null && notification.getId() != null) {
                    log.info("[NOTIFICATION] Publishing SSE event (failed email): userId={}, notificationId={}", 
                             notification.getUserId(), notification.getId());
                    notificationStreamService.publishNotification(notification.getUserId(), notification.getId());
                }
                throw new RuntimeException("SMTP send failed", e);
            }
        }
        notification = notificationRepository.save(notification);
        
        if (notification.getUserId() != null && notification.getId() != null) {
            log.info("[NOTIFICATION] Publishing SSE notification: userId={}, notificationId={}, status={}", 
                     notification.getUserId(), notification.getId(), notification.getStatus());
            notificationStreamService.publishNotification(notification.getUserId(), notification.getId());
        } else {
            log.warn("[NOTIFICATION] Skipping SSE publish: userId={}, notificationId={}", 
                     notification.getUserId(), notification.getId());
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setTo(to); h.setSubject(subject); h.setText(html, true);
        mailSender.send(msg);
    }

    private String buildHtml(NotificationRequest req) {
        String theme = themeFor(req.getType());
        String accent = accentFor(req.getType());
        String icon = iconFor(req.getType());
        String logoHtml = buildLogoHtml();
        String subject = escapeHtml(defaultText(req.getSubject(), "Update from CareerBridge"));
        String greetingName = escapeHtml(defaultText(req.getUserName(), "there"));
        String body = buildBody(req);
        String snapshot = buildSnapshot(req);
        String details = buildDetails(req);
        String normalizedType = normalizeType(req.getType());
        String ctaLabel = escapeHtml(defaultText(req.getCtaLabel(), defaultCtaLabel(req.getType())));
        String ctaUrl = escapeHtml(resolveCtaUrl(req.getCtaUrl(), normalizedType));
        String subtitle = formatMultilineText(subtitleFor(req));
        boolean shouldShowCta = !"EMAIL_VERIFICATION".equals(normalizedType);
        boolean isCenterMark = "EMAIL_VERIFICATION".equals(normalizedType);

        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>" + subject + "</title><style>"
            + "body{margin:0;padding:0;background:linear-gradient(180deg,#f8fafc 0%,#eef2ff 100%);font-family:Inter,Segoe UI,Arial,sans-serif;color:#0f172a;word-break:break-word;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%}"
            + ".shell{width:100%;padding:36px 16px;box-sizing:border-box}"
            + ".card{max-width:640px;margin:0 auto;background:#ffffff;border-radius:28px;overflow:hidden;box-shadow:0 24px 60px rgba(15,23,42,.12);border:1px solid #e5e7eb}"
            + ".hero{background:linear-gradient(135deg," + theme + " 0%, #0f172a 100%);padding:40px 42px;color:#fff;" + (isCenterMark ? "text-align:center;" : "") + "}"
            + ".brand{display:inline-block;background:rgba(255,255,255,.12);backdrop-filter:blur(10px);border:1px solid rgba(255,255,255,.16);border-radius:999px;padding:8px 14px;font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;" + (isCenterMark ? "margin:0 auto;" : "") + "}"
            + ".mark{width:44px;height:44px;border-radius:14px;display:block;line-height:44px;text-align:center;background:rgba(255,255,255,.15);font-size:16px;font-weight:800;letter-spacing:.04em;margin:22px 0 14px}"
            + ".title{margin:0;font-size:30px;line-height:1.2;font-weight:800;letter-spacing:-.03em}"
            + ".subtitle{margin:12px 0 0;font-size:15px;line-height:1.7;color:rgba(255,255,255,.82);max-width:540px}"
            + ".content{padding:34px 42px 8px}"
            + ".greeting{margin:0 0 14px;font-size:16px;line-height:1.6;color:#0f172a;font-weight:600}"
            + ".message{margin:0;font-size:15px;line-height:1.8;color:#334155;overflow-wrap:anywhere}"
            + ".message p{margin:0 0 14px}"
            + ".message p:last-child{margin-bottom:0}"
            + ".callout{margin-top:18px;padding:16px 18px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:16px;color:#1e3a8a;font-weight:600;overflow-wrap:anywhere}"
            + ".callout strong{display:block;margin-bottom:4px;color:#0f172a;text-transform:uppercase;letter-spacing:.06em;font-size:11px}"
            + ".snapshot{margin:0 0 18px;padding:18px 20px;border-radius:20px;background:linear-gradient(180deg,#f8fafc 0%,#ffffff 100%);border:1px solid #dbeafe;box-shadow:0 10px 22px rgba(15,23,42,.06)}"
            + ".snapshotHeader{margin-bottom:12px}"
            + ".snapshotTitle{margin:0;color:#0f172a;font-size:13px;font-weight:800;letter-spacing:.08em;text-transform:uppercase}"
            + ".snapshotTag{display:inline-block;margin-top:8px;border-radius:999px;padding:6px 10px;background:#ccfbf1;color:#0f766e;font-size:11px;font-weight:800;letter-spacing:.05em;text-transform:uppercase}"
            + ".snapshotGrid{display:block}"
            + ".snapshotItem{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:12px 14px;margin:0 0 12px}"
            + ".snapshotItem:last-child{margin-bottom:0}"
            + ".snapshotLabel{display:block;margin-bottom:4px;color:#64748b;font-size:11px;font-weight:700;letter-spacing:.06em;text-transform:uppercase}"
            + ".snapshotValue{display:block;color:#0f172a;font-size:14px;font-weight:700;line-height:1.5;word-break:break-word;overflow-wrap:anywhere}"
            + ".details{margin:24px 0 0;padding:18px 20px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:18px}"
            + ".detailRow{display:block;padding:10px 0;border-bottom:1px solid #e2e8f0}"
            + ".detailRow:last-child{border-bottom:0;padding-bottom:0}"
            + ".detailLabel{display:block;color:#64748b;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.06em}"
            + ".detailValue{display:block;margin-top:4px;color:#0f172a;font-size:14px;font-weight:600;text-align:left;word-break:break-word;overflow-wrap:anywhere}"
            + ".ctaWrap{padding:28px 42px 34px;text-align:center}"
            + ".cta{display:block;max-width:240px;margin:0 auto;background:" + theme + ";color:#ffffff !important;text-decoration:none;font-weight:700;font-size:14px;line-height:48px;border-radius:14px;text-align:center;box-shadow:0 10px 24px rgba(37,99,235,.18)}"
            + ".footnote{padding:0 42px 8px;color:#64748b;font-size:12px;line-height:1.7}"
            + ".footer{background:#f8fafc;padding:18px 42px 22px;text-align:center;border-top:1px solid #e5e7eb}"
            + ".footer p{margin:0;color:#94a3b8;font-size:12px;line-height:1.7}"
            + ".support{color:#64748b;font-size:12px;margin:0 0 8px}"
            + "@media (max-width:640px){.shell{padding:18px 10px}.card{border-radius:22px}.hero,.content,.ctaWrap,.footnote,.footer{padding-left:20px;padding-right:20px}.title{font-size:24px}.subtitle{font-size:14px}.cta{max-width:none;width:100%;box-sizing:border-box}.snapshot,.details{padding-left:16px;padding-right:16px}}"
            + "</style></head><body><div class=\"shell\"><div class=\"card\">"
            + "<div class=\"hero\">" + logoHtml
            + "<div class=\"mark\" style=\"background:" + accent + ";color:" + theme + (isCenterMark ? ";margin-left:auto;margin-right:auto;" : "") + "\">" + icon + "</div>"
            + "<h1 class=\"title\">" + subject + "</h1>"
            + "<p class=\"subtitle\">" + subtitle + "</p></div>"
            + "<div class=\"content\"><p class=\"greeting\">Hi " + greetingName + ",</p>" + snapshot + "<div class=\"message\">" + body + "</div>" + details + "</div>"
            + (shouldShowCta ? "<div class=\"ctaWrap\"><a class=\"cta\" href=\"" + ctaUrl + "\" target=\"_blank\" rel=\"noreferrer\" style=\"background:" + theme + ";color:#ffffff !important;text-decoration:none;display:block;max-width:240px;margin:0 auto;line-height:48px;text-align:center;font-weight:700;font-size:14px;border-radius:14px;box-shadow:0 10px 24px rgba(37,99,235,.18);\">" + ctaLabel + "</a></div>" : "")
            + "<div class=\"footnote\"><p class=\"support\">Need help? Reply to this email or contact support from your CareerBridge dashboard.</p><p>You are receiving this email because your CareerBridge account is set up for notifications.</p></div>"
            + "<div class=\"footer\"><p>© 2026 CareerBridge. All rights reserved.<br>If you do not want these emails, you can update your notification preferences in the app.</p></div>"
            + "</div></div></body></html>";
    }

    private String buildSnapshot(NotificationRequest req) {
        if (!"NEW_APPLICATION".equals(normalizeType(req.getType()))) {
            return "";
        }

        return "<div class=\"snapshot\"><div class=\"snapshotHeader\">"
            + "<p class=\"snapshotTitle\">Recruiter Snapshot</p>"
            + "<span class=\"snapshotTag\">New Application</span>"
            + "</div><div class=\"snapshotGrid\">"
            + snapshotItem("Job", defaultText(req.getJobTitle(), "N/A"))
            + snapshotItem("Company", defaultText(req.getCompanyName(), "N/A"))
            + snapshotItem("Candidate", defaultText(req.getUserName(), "A new applicant"))
            + snapshotItem("Status", defaultText(req.getStatus(), "Submitted"))
            + "</div></div>";
    }

    private String snapshotItem(String label, String value) {
        return "<div class=\"snapshotItem\"><span class=\"snapshotLabel\">" + escapeHtml(label)
            + "</span><span class=\"snapshotValue\">" + escapeHtml(value) + "</span></div>";
    }

    private String buildLogoHtml() {
        String logoUrl = frontendBaseUrl.replaceAll("/+$", "") + "/careerbridge-logo.svg";
        return "<div class=\"brand\"><img src=\"" + escapeHtml(logoUrl)
            + "\" alt=\"CareerBridge\" style=\"display:block;height:30px;width:auto;max-width:220px;\" /></div>";
    }

    private String buildBody(NotificationRequest req) {
        String normalizedType = normalizeType(req.getType());
        String body = defaultText(req.getBody(), "We have an update for your CareerBridge account.");

        return switch (normalizedType) {
            case "JOB_APPLIED" ->
                "<p>Your application for <strong>" + escapeHtml(defaultText(req.getJobTitle(), "this role"))
                    + "</strong> at <strong>" + escapeHtml(defaultText(req.getCompanyName(), "the employer"))
                    + "</strong> has been received successfully.</p>"
                + "<p>The recruiter can now review your profile. We will notify you when there is an update.</p>"
                + (body != null && !body.isBlank() ? "<div class=\"callout\"><strong>Summary</strong>"
                    + formatMultilineText(body) + "</div>" : "");
            case "NEW_APPLICATION" ->
                "<p>A new candidate has applied for <strong>" + escapeHtml(defaultText(req.getJobTitle(), "your job post"))
                    + "</strong> at <strong>" + escapeHtml(defaultText(req.getCompanyName(), "CareerBridge"))
                    + "</strong>.</p>"
                + "<p>Please review the candidate profile, resume, and application details to decide the next step.</p>"
                + (body != null && !bodyBlank(body) ? "<div class=\"callout\"><strong>Candidate note</strong>"
                    + formatMultilineText(body) + "</div>" : "");
            case "STATUS_CHANGED" ->
                "<p>Your application for <strong>" + escapeHtml(defaultText(req.getJobTitle(), "this role"))
                    + "</strong> has been updated to <strong>" + escapeHtml(defaultText(req.getStatus(), "an updated status"))
                    + "</strong>.</p>"
                + "<p>Please review the latest update in your account. If the recruiter left a note, you’ll find it in your application details.</p>"
                + (body != null && !body.isBlank() ? "<div class=\"callout\"><strong>Update</strong>"
                    + formatMultilineText(body) + "</div>" : "");
            case "APPLICATION_WITHDRAWN" ->
                "<p>Your application has been withdrawn successfully.</p>"
                + "<p>You can continue exploring new roles and apply again when you find a better match.</p>"
                + (body != null && !bodyBlank(body) ? "<div class=\"callout\"><strong>Details</strong>"
                    + formatMultilineText(body) + "</div>" : "");
            case "WELCOME" ->
                "<p>Welcome to <strong>CareerBridge</strong>. Your account is now active and ready to use.</p>"
                + "<p>You can browse jobs, manage your profile, and apply for opportunities that match your skills.</p>"
                + (body != null && !bodyBlank(body) ? "<div class=\"callout\"><strong>Getting started</strong>"
                    + formatMultilineText(body) + "</div>" : "");
            case "EMAIL_VERIFICATION" ->
                "<p>Please verify your email address to complete your CareerBridge account setup.</p>"
                + "<div class=\"callout\"><strong>Verification code</strong>"
                + formatMultilineText(body) + "</div>";
            case "JOB_DELETED" ->
                "<p>A job you applied for is no longer available on CareerBridge.</p>"
                + "<p>We understand this can be disappointing. Please continue exploring other roles that fit your background.</p>"
                + (body != null && !bodyBlank(body) ? "<div class=\"callout\"><strong>Details</strong>"
                    + formatMultilineText(body) + "</div>" : "");
            case "ACCOUNT_STATUS" ->
                "<p>Your CareerBridge account status has been updated.</p>"
                + "<p>If you did not expect this change, please contact support for help.</p>"
                + (body != null && !bodyBlank(body) ? "<div class=\"callout\"><strong>Status note</strong>"
                    + formatMultilineText(body) + "</div>" : "");
            case "ACCOUNT_DELETED" ->
                "<p>Your CareerBridge account has been removed.</p>"
                + "<p>Thank you for being part of CareerBridge. If you believe this was an error, please contact support.</p>"
                + (body != null && !bodyBlank(body) ? "<div class=\"callout\"><strong>Note</strong>"
                    + formatMultilineText(body) + "</div>" : "");
            default -> "<p>" + formatMultilineText(body) + "</p>";
        };
    }

    private String themeFor(String type) {
        return switch (normalizeType(type)) {
            case "JOB_APPLIED" -> "#16a34a";
            case "STATUS_CHANGED" -> "#2563eb";
            case "APPLICATION_WITHDRAWN" -> "#b45309";
            case "WELCOME", "EMAIL_VERIFICATION" -> "#7c3aed";
            case "NEW_APPLICATION" -> "#0f766e";
            case "JOB_DELETED" -> "#b45309";
            case "ACCOUNT_STATUS" -> "#0f766e";
            case "ACCOUNT_DELETED" -> "#991b1b";
            default -> "#0f172a";
        };
    }

    private String accentFor(String type) {
        return switch (normalizeType(type)) {
            case "JOB_APPLIED" -> "#dcfce7";
            case "STATUS_CHANGED" -> "#dbeafe";
            case "APPLICATION_WITHDRAWN" -> "#ffedd5";
            case "WELCOME", "EMAIL_VERIFICATION" -> "#ede9fe";
            case "NEW_APPLICATION" -> "#ccfbf1";
            case "JOB_DELETED" -> "#ffedd5";
            case "ACCOUNT_STATUS" -> "#ccfbf1";
            case "ACCOUNT_DELETED" -> "#fee2e2";
            default -> "#e2e8f0";
        };
    }

    private String iconFor(String type) {
        return switch (normalizeType(type)) {
            case "JOB_APPLIED" -> "AP";
            case "STATUS_CHANGED" -> "UP";
            case "APPLICATION_WITHDRAWN" -> "WD";
            case "WELCOME" -> "WB";
            case "EMAIL_VERIFICATION" -> "OTP";
            case "NEW_APPLICATION" -> "NA";
            case "JOB_DELETED" -> "JD";
            case "ACCOUNT_STATUS" -> "AS";
            case "ACCOUNT_DELETED" -> "AD";
            default -> "TB";
        };
    }

    private String subtitleFor(NotificationRequest req) {
        return switch (normalizeType(req.getType())) {
            case "JOB_APPLIED" -> joinSentences(
                "Your application has been received and recorded successfully.",
                "We will keep you updated as the recruiter reviews your profile.");
            case "STATUS_CHANGED" -> joinSentences(
                "Your application status has been updated.",
                "Please review the latest details in your CareerBridge account.");
            case "APPLICATION_WITHDRAWN" -> joinSentences(
                "Your application withdrawal has been completed.",
                "You can continue applying to other opportunities on CareerBridge.");
            case "WELCOME" -> joinSentences(
                "Your account is ready.",
                "Start exploring roles and building your job search profile.");
            case "EMAIL_VERIFICATION" -> joinSentences(
                "Use the code below to verify your email address.",
                "This helps us keep your account secure.");
            case "JOB_DELETED" -> joinSentences(
                "A job you followed has been removed from the platform.",
                "Please explore other open opportunities on CareerBridge.");
            case "ACCOUNT_STATUS" -> joinSentences(
                "Your account status has changed.",
                "If this looks unexpected, please contact support.");
            case "ACCOUNT_DELETED" -> joinSentences(
                "Your account has been removed from CareerBridge.",
                "We appreciate the time you spent with us.");
            default -> "You have a new update in your CareerBridge account.";
        };
    }

    private String buildDetails(NotificationRequest req) {
        StringBuilder details = new StringBuilder("<div class=\"details\">");
        boolean hasDetails = false;
        hasDetails |= appendDetail(details, "Job", req.getJobTitle());
        hasDetails |= appendDetail(details, "Company", req.getCompanyName());
        hasDetails |= appendDetail(details, "Status", req.getStatus());
        hasDetails |= appendDetail(details, "Reference", req.getApplicationId() != null ? String.valueOf(req.getApplicationId()) : null);
        details.append(hasDetails ? "</div>" : "</div>");
        return hasDetails ? details.toString() : "";
    }

    private boolean appendDetail(StringBuilder html, String label, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        html.append("<div class=\"detailRow\"><span class=\"detailLabel\">")
            .append(escapeHtml(label))
            .append("</span><span class=\"detailValue\">")
            .append(escapeHtml(value))
            .append("</span></div>");
        return true;
    }

    private String defaultCtaLabel(String type) {
        return switch (normalizeType(type)) {
            case "JOB_APPLIED" -> "View Application";
            case "STATUS_CHANGED" -> "View Status";
            case "APPLICATION_WITHDRAWN" -> "Explore Jobs";
            case "WELCOME" -> "Open CareerBridge";
            case "NEW_APPLICATION" -> "Review Candidate";
            case "EMAIL_VERIFICATION" -> "Verify Email";
            case "JOB_DELETED" -> "Browse Jobs";
            case "ACCOUNT_STATUS" -> "Review Account";
            case "ACCOUNT_DELETED" -> "Contact Support";
            default -> "Open CareerBridge";
        };
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String resolveCtaUrl(String rawUrl, String normalizedType) {
        String baseUrl = defaultText(frontendBaseUrl, "http://localhost:3000").replaceAll("/+$", "");

        if (rawUrl != null && !rawUrl.isBlank()) {
            String sanitized = rawUrl.trim();
            if (sanitized.contains("careerbridge.local")) {
                sanitized = sanitized.replace("https://careerbridge.local", baseUrl)
                    .replace("http://careerbridge.local", baseUrl);
            }

            if (sanitized.regionMatches(true, 0, "javascript:", 0, "javascript:".length())
                || sanitized.regionMatches(true, 0, "data:", 0, "data:".length())) {
                return baseUrl;
            }

            if (sanitized.startsWith("/")) {
                return baseUrl + sanitized;
            }

            boolean hasScheme = sanitized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");
            if (!hasScheme) {
                return baseUrl + "/" + sanitized.replaceAll("^/+", "");
            }

            return sanitized;
        }

        return switch (normalizedType) {
            case "JOB_APPLIED" -> baseUrl + "/applications";
            case "STATUS_CHANGED" -> baseUrl + "/applications";
            case "APPLICATION_WITHDRAWN" -> baseUrl + "/jobs";
            case "WELCOME", "EMAIL_VERIFICATION" -> baseUrl + "/login";
            case "NEW_APPLICATION" -> baseUrl + "/recruiter/applications";
            case "JOB_DELETED" -> baseUrl + "/jobs";
            case "ACCOUNT_STATUS", "ACCOUNT_DELETED" -> baseUrl + "/profile";
            default -> baseUrl;
        };
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean bodyBlank(String value) {
        return normalizeUserText(value).isBlank();
    }

    private String joinSentences(String first, String second) {
        return first + "\n\n" + second;
    }

    private String formatMultilineText(String value) {
        return escapeHtml(normalizeUserText(value)).replace("\n", "<br>");
    }

    private String normalizeUserText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replaceAll("(?i)<br\\s*/?>", "\n")
            .replaceAll("(?i)^\\s*type\\s*[:=-]?\\s*", "");

        return normalized.trim();
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserId(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribe(Long userId) {
        return notificationStreamService.subscribe(userId);
    }

    @Transactional
    public boolean markAsReadByReference(Long userId, String notificationRef) {
        Long notificationId = resolveNotificationId(notificationRef, userId);
        if (notificationId == null) {
            return false;
        }
        return notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now()) > 0;
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Transactional
    public long clearUserNotifications(Long userId) {
        return notificationRepository.deleteByUserId(userId);
    }

    @Transactional
    public boolean deleteUserNotificationByReference(Long userId, String notificationRef) {
        Long notificationId = resolveNotificationId(notificationRef, userId);
        if (notificationId == null) {
            return false;
        }
        return notificationRepository.deleteByIdAndUserId(notificationId, userId) > 0;
    }

    private Long resolveNotificationId(String notificationRef, Long userId) {
        if (notificationRef == null || notificationRef.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(notificationRef);
        } catch (NumberFormatException ignored) {
            return notificationRepository.findByUuidAndUserId(notificationRef, userId)
                .map(Notification::getId)
                .orElse(null);
        }
    }
}
