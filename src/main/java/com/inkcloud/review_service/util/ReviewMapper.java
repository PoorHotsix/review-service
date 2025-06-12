package com.inkcloud.review_service.util;

import com.inkcloud.review_service.domain.Review;
import com.inkcloud.review_service.dto.ReviewDto;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewDto entityToDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .email(review.getEmail())
                .productId(review.getProductId())
                .productName(review.getProductName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    public Review dtoToEntity(ReviewDto dto) {
        return Review.builder()
                .email(dto.getEmail())
                .productId(dto.getProductId())
                .productName(dto.getProductName())
                .rating(dto.getRating())
                .comment(dto.getComment())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
