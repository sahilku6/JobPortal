package com.jobportal.notification.model;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "notifications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, updatable = false, length = 36)
    private String uuid;
    @Column(name = "user_id") private Long userId;
    @Column(name = "application_id") private Long applicationId;
    @Column(name = "recipient_email") private String to;
    private String subject;
    @Column(columnDefinition = "TEXT") private String body;
    private String type;
    @Enumerated(EnumType.STRING) @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;
    @Column(name = "is_read") @Builder.Default
    private Boolean isRead = false;
    @Column(name = "read_at") private LocalDateTime readAt;
    @Column(name = "error_message") private String errorMessage;
    @Column(name = "sent_at")
    @JsonSerialize(using = LocalDateTimeToInstantSerializer.class)
    private LocalDateTime sentAt;
    @Column(name = "created_at")
    @JsonSerialize(using = LocalDateTimeToInstantSerializer.class)
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() {
        if (uuid == null || uuid.isBlank()) uuid = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }
    public enum NotificationStatus { PENDING, SENT, FAILED }
}

class LocalDateTimeToInstantSerializer extends com.fasterxml.jackson.databind.JsonSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen, 
            com.fasterxml.jackson.databind.SerializerProvider serializers) throws java.io.IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            // Convert LocalDateTime to Instant (UTC)
            Instant instant = value.atZone(ZoneId.systemDefault()).toInstant();
            gen.writeString(instant.toString());
        }
    }
}
