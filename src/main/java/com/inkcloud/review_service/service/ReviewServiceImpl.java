package com.inkcloud.review_service.service;

import com.inkcloud.review_service.domain.Review;
import com.inkcloud.review_service.dto.ReviewDto;
import com.inkcloud.review_service.dto.ReviewEventDto;
import com.inkcloud.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    private final KafkaTemplate<String, ReviewEventDto> kafkaTemplate;

    @Value("${kafka.topic.review-rating-update:review-rating-update}")
    private String reviewRatingUpdateTopic;

    private final ObjectMapper objectMapper = new ObjectMapper();

    //리뷰 작성 
    @Override
    public void createReview(ReviewDto reviewDto, String email) {
        reviewDto.setEmail(email); 
        Review review = dtoToEntity(reviewDto);
        reviewRepository.save(review);

        // 카프카 메시지 전송
        ReviewEventDto event = new ReviewEventDto("created", review.getProductId(), review.getRating(), null);
        log.info("카프카 메시지 전송 완료: {}", event);
        sendRatingUpdateMessage(event);
    }

    // 책 ID로 리뷰 리스트 조회
    @Override
    public List<ReviewDto> getReviewsByProductId(String productId) {
        List<Review> reviews = reviewRepository.findAllByProductId(productId);
        return reviews.stream()
                .map(this::entityToDto)
                .toList();
    }

    // 회원 이메일로 리뷰 리스트 조회
    @Override
    public List<ReviewDto> getReviewsByEmail(String email) {
        List<Review> reviews = reviewRepository.findAllByEmail(email);
        return reviews.stream()
                .map(this::entityToDto)
                .toList();
    }

    // 전체 리뷰 조회-관리자
    @Override
    public List<ReviewDto> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();
        return reviews.stream()
                .map(this::entityToDto)
                .toList();
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

    //상품 리뷰 평균 조회
    @Override
    public double getAverageRatingByProductId(String productId) {
        List<Review> reviews = reviewRepository.findAllByProductId(productId);
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    // 전체 리뷰 조회 + 필터링 (관리자)
    @Override
    public Page<ReviewDto> getAllReviewsWithFilter(
            int page, int size, String keyword, String startDate, String endDate, Integer minRating, Integer maxRating) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Specification 사용 (JPA Criteria)
        Specification<Review> spec = Specification.where(null);

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(root.get("productName"), "%" + keyword + "%"),
                cb.like(root.get("comment"), "%" + keyword + "%")
            ));
        }
        if (startDate != null && endDate != null) {
            spec = spec.and((root, query, cb) -> cb.between(
                root.get("createdAt"),
                LocalDateTime.parse(startDate),
                LocalDateTime.parse(endDate)
            ));
        }
        if (minRating != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), minRating));
        }
        if (maxRating != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("rating"), maxRating));
        }

        Page<Review> reviewPage = reviewRepository.findAll(spec, pageable);
        return reviewPage.map(this::entityToDto);
    }

    // 리뷰 작성/수정/삭제시, 카프카로 메시지 전송
    private void sendRatingUpdateMessage(ReviewEventDto reviewEventDto) {
        kafkaTemplate.send("review-rating-update", reviewEventDto);
    }
}
