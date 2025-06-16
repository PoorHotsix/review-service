package com.inkcloud.review_service.service;

import com.inkcloud.review_service.domain.Review;
import com.inkcloud.review_service.domain.ReviewLike;
import com.inkcloud.review_service.dto.ReviewDto;
import com.inkcloud.review_service.dto.ReviewEventDto;
import com.inkcloud.review_service.dto.ReviewLikeDto;
import com.inkcloud.review_service.repository.ReviewLikeRepository;
import com.inkcloud.review_service.repository.ReviewRepository;
import com.inkcloud.review_service.util.ReviewMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewMapper reviewMapper;

    private final KafkaTemplate<String, ReviewEventDto> kafkaTemplate;

    @Value("${kafka.topic.review-rating-update:review-rating-update}")
    private String reviewRatingUpdateTopic;

    // private final ObjectMapper objectMapper = new ObjectMapper();

    //리뷰 작성 
    @Override
    public boolean createReview(ReviewDto reviewDto, String email) {
        Optional<Review> existing = reviewRepository.findByProductIdAndEmail(reviewDto.getProductId(), email);
        if (existing.isPresent()) {
            // 이미 작성한 리뷰가있으면 false 반환
            reviewDto.setId(existing.get().getId());
            return false;
        }
        reviewDto.setEmail(email); 
        Review review = reviewMapper.dtoToEntity(reviewDto);
        reviewRepository.save(review);

        // 카프카 메시지 전송
        ReviewEventDto event = new ReviewEventDto("created", review.getProductId(), review.getRating(), null);
        log.info("카프카 메시지 전송 완료: {}", event);
        sendRatingUpdateMessage(event);
        return true;
    }

    // 책 ID로 리뷰 리스트 조회
    @Override
    public List<ReviewDto> getReviewsByProductId(Long productId) {
        List<Review> reviews = reviewRepository.findAllByProductId(productId);
        return reviews.stream()
                .map(reviewMapper::entityToDto)
                .toList();
    }



    //책 ID + 회원 이메일로 리뷰 리스트(좋아요 여부 포함) 조회
    @Override
    public List<ReviewDto> getReviewsWithLikes(Long productId, String email) {
        // 1. 해당 책의 모든 리뷰 조회
        List<Review> reviews = reviewRepository.findAllByProductId(productId);

        // 2. 사용자가 좋아요한 리뷰 ID 목록 조회
        List<Long> likedReviewIds = reviewLikeRepository.findAllByEmail(email).stream()
                .map(like -> like.getReview().getId())
                .toList();

        // 3. 각 리뷰를 ReviewDto로 변환하면서 likedByMe 필드 세팅
        return reviews.stream()
                .map(review -> {
                    ReviewDto dto = reviewMapper.entityToDto(review);
                    dto.setLikedByMe(likedReviewIds.contains(review.getId()));
                    return dto;
                })
                .toList();
    }

    // 회원 이메일로 리뷰 리스트 조회
    @Override
    public List<ReviewDto> getReviewsByEmail(String email, String period) {

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        switch (period) {
            case "1d": startDate = today; break; // 오늘
            case "1m": startDate = today.minusMonths(1); break; //1개월
            case "3m": startDate = today.minusMonths(3); break; //3개월
            case "6m": startDate = today.minusMonths(6); break; //6개월
            case "5y": startDate = today.minusYears(5); break; //6개월
            
            default: startDate = today.minusYears(5); // 전체
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1); // 오늘 23:59:59.999999999
        List<Review> reviews = reviewRepository.findByEmailAndCreatedAtBetween(email, start, end);
        return reviews.stream()
                .map(reviewMapper::entityToDto)
                .toList();
    }

    //리뷰 상세 조회
    @Override
    public ReviewDto getReviewDetail(Long reviewId, String email) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // email이 null 또는 빈 값이면(관리자) 본인 체크 생략
        if (email != null && !email.isBlank()) {
            if (!review.getEmail().equals(email)) {
                throw new AccessDeniedException("본인 리뷰만 조회할 수 있습니다.");
            }
        }
        return reviewMapper.entityToDto(review);
    }

    // 리뷰 수정 (내용, 별점만)
    @Override
    public void updateReview(Long reviewId, ReviewDto reviewDto, String email) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!review.getEmail().equals(email)) {
            throw new IllegalArgumentException("본인 리뷰만 수정할 수 있습니다.");
        }

        // 코멘트만 변경된 경우
        if (reviewDto.getComment() != null) {
            review.setComment(reviewDto.getComment());
            reviewRepository.save(review);
        }

        // 평점이 변경된 경우
        if (reviewDto.getRating() != null && !reviewDto.getRating().equals(review.getRating())) {
            int oldRating = review.getRating();
            review.setRating(reviewDto.getRating());
            reviewRepository.save(review);

            ReviewEventDto event = new ReviewEventDto("updated", review.getProductId(), reviewDto.getRating(), oldRating);
            log.info("리뷰 수정 - 카프카 메시지 전송: {}", event);
            sendRatingUpdateMessage(event);
        }
    }



    // 리뷰 삭제 (여러 개 또는 하나 삭제 가능)
    @Override
    public void deleteReviews(List<Long> reviewIds, String email, List<String> roles) {
        boolean isAdmin = roles != null && roles.contains("ADMIN");

        for (Long reviewId : reviewIds) {
            Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

            boolean isOwner = review.getEmail().equals(email);

            if (!isOwner && !isAdmin) {
                throw new IllegalArgumentException("본인 또는 관리자만 삭제할 수 있습니다.");
            }

            //카프카 메세지 전송
            ReviewEventDto event = new ReviewEventDto("deleted",review.getProductId(), null ,review.getRating()); // 삭제이므로 oldRating에 기존 평점 전달
            log.info("리뷰 삭제 - 카프카 메시지 전송: {}", event);
            sendRatingUpdateMessage(event);

            reviewRepository.delete(review);
        }
    }

    // 전체 리뷰 조회 + 필터링 (관리자)
    @Override
    public Page<ReviewDto> getAllReviewsWithFilter(
            int page, int size, String keyword, String startDate, String endDate, Integer minRating, Integer maxRating) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepository.searchReviews(keyword, startDate, endDate, minRating, maxRating, pageable);
    }

    // 리뷰 작성/수정/삭제시, 카프카로 메시지 전송
    private void sendRatingUpdateMessage(ReviewEventDto reviewEventDto) {
        kafkaTemplate.send("review-rating-update", reviewEventDto);
    }

    //리뷰 좋아요
    @Override
    public void likesReview(Long reviewId, String email) {

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

    // 이미 좋아요를 눌렀는지 확인
    boolean alreadyLiked = reviewLikeRepository.existsByReviewIdAndEmail(reviewId, email);
    if (alreadyLiked) {
        throw new IllegalStateException("이미 좋아요를 누른 리뷰입니다.");
    }

    // 좋아요 추가
    ReviewLike reviewLike = ReviewLike.builder()
        .review(review)
        .email(email)
        .createdAt(LocalDateTime.now())
        .build();

    reviewLikeRepository.save(reviewLike);

    // likeCount 증가
    review.setLikeCount(review.getLikeCount() + 1);
    reviewRepository.save(review);
    }

    //리뷰 좋아요 취소 
    @Override
    public void cancelLikesReview(Long reviewId, String email) {

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

    // 좋아요 엔티티 찾기
    ReviewLike reviewLike = reviewLikeRepository.findByReviewAndEmail(review, email)
        .orElseThrow(() -> new IllegalStateException("좋아요를 누른 적이 없습니다."));

    reviewLikeRepository.delete(reviewLike);

    // likeCount 감소 (0 이하로 내려가지 않게)
    review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
    reviewRepository.save(review);
    }
}
