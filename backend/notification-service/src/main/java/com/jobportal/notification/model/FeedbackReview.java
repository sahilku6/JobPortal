package com.jobportal.notification.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "feedback_reviews")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FeedbackReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "user_role", nullable = false, length = 50)
    private String userRole;

    @Column(name = "review_text", columnDefinition = "TEXT", nullable = false)
    private String reviewText;

    @Column(nullable = false)
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public enum TriggerType { FIRST_JOB_POST, FIRST_JOB_APPLY }
}
