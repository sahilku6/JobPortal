package com.jobportal.notification.repository;

import com.jobportal.notification.model.FeedbackReview;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<FeedbackReview, Long> {
    @Query("SELECT f FROM FeedbackReview f ORDER BY f.rating DESC, f.createdAt DESC")
    List<FeedbackReview> findTopReviews(Pageable pageable);

    default List<FeedbackReview> findTopReviews(int limit) {
        return findTopReviews(PageRequest.of(0, limit));
    }
}
