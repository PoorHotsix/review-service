package com.inkcloud.review_service.controller;

import com.inkcloud.review_service.dto.ReviewDto;
import com.inkcloud.review_service.dto.ReviewRequestDto;
import com.inkcloud.review_service.service.ReviewService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성 (JWT 토큰에서 email 추출)
    @PostMapping
    public ResponseEntity<String> createReview(@RequestBody ReviewDto reviewDto,
                                               @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        try {
            reviewService.createReview(reviewDto, email);
            return ResponseEntity.ok("리뷰가 성공적으로 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            // 한 회원이 같은 책에 대한 리뷰 작성시 409에러 반환
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    // 책 ID로 리뷰 리스트, 평균 조회
    @GetMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> getReviewsByProductId(@PathVariable String productId) {

        List<ReviewDto> reviews = reviewService.getReviewsByProductId(productId);
        double avgRating = reviewService.getAverageRatingByProductId(productId);

        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviews);
        result.put("averageRating", avgRating);

        return ResponseEntity.ok(result);
    }

    // 회원 이메일로 리뷰 리스트 조회 (JWT 토큰에서 email 추출)
    @GetMapping("/members/me")
    public ResponseEntity<List<ReviewDto>> getReviewsByEmail(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        List<ReviewDto> reviews = reviewService.getReviewsByEmail(email);
        return ResponseEntity.ok(reviews);
    }

    // 리뷰 상세 조회 (JWT 토큰에서 email 추출, 본인만 조회 가능)
    @GetMapping("/detail/{reviewId}")
    public ResponseEntity<ReviewDto> getReviewDetail(@PathVariable Long reviewId,
                                                     @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        try {
            ReviewDto review = reviewService.getReviewDetail(reviewId, email);
            return ResponseEntity.ok(review);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).build();
        }
    }

    // 리뷰 수정 (comment, rating만, 둘 중 하나만 수정도 가능)
    @PatchMapping("/{reviewId}")
    public ResponseEntity<String> updateReview(@PathVariable Long reviewId,
                                               @RequestBody ReviewDto reviewDto,
                                               @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        reviewService.updateReview(reviewId, reviewDto, email);
        return ResponseEntity.ok("리뷰가 성공적으로 수정되었습니다.");
    }

    // 리뷰 삭제 (여러 개 또는 하나 삭제 가능)
    @DeleteMapping
    public ResponseEntity<String> deleteReviews(
            @RequestBody List<Long> reviewIds,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> roles = (List<String>) realmAccess.getOrDefault("roles", Collections.emptyList());

        reviewService.deleteReviews(reviewIds, email, roles);
        return ResponseEntity.ok("리뷰가 성공적으로 삭제되었습니다.");
    }

    // 전체 리뷰 검색-관리자
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/admin")
    public ResponseEntity<Page<ReviewDto>> searchAllReviews(@RequestBody ReviewRequestDto req) {
        Page<ReviewDto> reviews = reviewService.getAllReviewsWithFilter(
            req.getPage(), req.getSize(), req.getKeyword(), req.getStartDate(), req.getEndDate(), req.getMinRating(), req.getMaxRating()
        );
        return ResponseEntity.ok(reviews);
    }
}
