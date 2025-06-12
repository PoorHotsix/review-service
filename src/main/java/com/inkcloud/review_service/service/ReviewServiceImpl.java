package com.inkcloud.review_service.service;

import com.inkcloud.review_service.domain.Review;
import com.inkcloud.review_service.dto.ReviewDto;
import com.inkcloud.review_service.dto.ReviewEventDto;
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

import com.fasterxml.jackson.databind.ObjectMapper;

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
        if (!review.getEmail().equals(email)) {
            throw new AccessDeniedException("본인 리뷰만 조회할 수 있습니다.");
        }
        return reviewMapper.entityToDto(review);
    }

    
    // 전체 리뷰 조회-관리자
    // @Override
    // public List<ReviewDto> getAllReviews() {
    //     List<Review> reviews = reviewRepository.findAll();
    //     return reviews.stream()
    //             .map(this::entityToDto)
    //             .toList();
    // }

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

    //상품 리뷰 평균 조회
    // @Override
    // public double getAverageRatingByProductId(Long productId) {
    //     List<Review> reviews = reviewRepository.findAllByProductId(productId);
    //     return reviews.stream()
    //             .mapToInt(Review::getRating)
    //             .average()
    //             .orElse(0.0);
    // }

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
}
