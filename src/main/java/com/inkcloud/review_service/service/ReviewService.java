package com.inkcloud.review_service.service;

import com.inkcloud.review_service.dto.ReviewDto;
import com.inkcloud.review_service.dto.ReviewLikeDto;

import org.springframework.data.domain.Page;

import java.util.List;

public interface ReviewService {

    //리뷰 작성 
    boolean createReview(ReviewDto reviewDto, String email);

    // 책 ID로 리뷰 리스트 조회
    List<ReviewDto> getReviewsByProductId(Long productId);

    //책 ID + 회원 이메리일로 뷰 리스트(좋아요 여부 포함) 조회
    List<ReviewDto> getReviewsWithLikes(Long productId, String email);

    // 회원 이메일로 리뷰 리스트 조회
    List<ReviewDto> getReviewsByEmail(String email, String period);

    //리뷰 상세 조회
    ReviewDto getReviewDetail(Long reviewId, String email);

    // 전체 리뷰 조회, 검색-관리자
    Page<ReviewDto> getAllReviewsWithFilter(
            int page, int size, String keyword, String startDate, String endDate, Integer minRating, Integer maxRating
    );

    // 리뷰 수정 
    void updateReview(Long reviewId, ReviewDto reviewDto, String email);

    // 리뷰 삭제
    void deleteReviews(List<Long> reviewIds, String email, List<String> roles);

    //리뷰 좋아요 
    void likesReview(Long reviewId, String email);

    //리뷰 좋아요 취소 
    void cancelLikesReview(Long reviewId, String email);


    // 상품 ID로 평균 평점 조회
    // double getAverageRatingByProductId(String productId);

    // DTO → Entity 변환
    // default Review dtoToEntity(ReviewDto dto) {
    //     return Review.builder()
    //             .email(dto.getEmail())
    //             .productId(dto.getProductId())
    //             .productName(dto.getProductName())
    //             .rating(dto.getRating())
    //             .comment(dto.getComment())
    //             .createdAt(dto.getCreatedAt())
    //             .updatedAt(dto.getUpdatedAt())
    //             .build();
    // }

    // // Entity → DTO 변환
    // default ReviewDto entityToDto(Review review) {
    //     return ReviewDto.builder()
    //             .id(review.getId())
    //             .email(review.getEmail())
    //             .productId(review.getProductId())
    //             .productName(review.getProductName())
    //             .rating(review.getRating())
    //             .comment(review.getComment())
    //             .createdAt(review.getCreatedAt())
    //             .updatedAt(review.getUpdatedAt())
    //             .build();
    // }




}
