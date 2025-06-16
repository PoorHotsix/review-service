package com.inkcloud.review_service.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.inkcloud.review_service.domain.Review;
import com.inkcloud.review_service.domain.ReviewLike;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    boolean existsByReviewIdAndEmail(Long reviewId, String email);

    Optional<ReviewLike> findByReviewAndEmail(Review review, String email);

    List<ReviewLike> findAllByEmail(String email);

    
}
