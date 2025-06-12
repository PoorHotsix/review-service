package com.inkcloud.review_service.repository;

import com.inkcloud.review_service.dto.ReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepositoryCustom {
    Page<ReviewDto> searchReviews(
        String keyword, String startDate, String endDate, Integer minRating, Integer maxRating, Pageable pageable
    );
}
