package com.jobportal.notification.service;

import com.jobportal.notification.model.FeedbackReview;
import com.jobportal.notification.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public void submit(Long userId, String reviewText, Integer rating,
                       String triggerType, String userName, String userRole) {
        FeedbackReview review = FeedbackReview.builder()
            .userId(userId)
            .userName(userName != null ? userName : "Anonymous")
            .userRole(userRole != null ? userRole : "User")
            .reviewText(reviewText.trim())
            .rating(rating)
            .triggerType(parseTrigger(triggerType))
            .build();
        feedbackRepository.save(review);
        log.info("Feedback saved userId={} trigger={}", userId, triggerType);
    }

    public List<FeedbackReview> getPublicReviews(int limit) {
        return feedbackRepository.findTopReviews(Math.min(limit, 50));
    }

    private FeedbackReview.TriggerType parseTrigger(String raw) {
        try { return FeedbackReview.TriggerType.valueOf(raw.toUpperCase()); }
        catch (Exception e) { return FeedbackReview.TriggerType.FIRST_JOB_APPLY; }
    }
}
