package com.inkcloud.review_service.controller;

import com.inkcloud.review_service.domain.ReportType;
import com.inkcloud.review_service.dto.ReviewReportDto;
import com.inkcloud.review_service.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewReportController {
    private final ReviewReportService reviewReportService;

    // 리뷰 신고
    @PostMapping("/report")
    public ResponseEntity<?> reportReview(@RequestBody ReviewReportDto dto, @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getClaimAsString("email");
        log.info("리뷰 신고 API 호출: reviewId={}, email={}, type={}, reason={}", dto.getReviewId(), email, dto.getType(), dto.getReason());
        
        try {
            reviewReportService.reportReview(dto.getReviewId(), email, dto.getType(), dto.getReason());
            log.info("리뷰 신고 처리 완료: reviewId={}, email={}", dto.getReviewId(), email);
            return ResponseEntity.ok("신고가 접수되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("리뷰 신고 실패(잘못된 요청): {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("리뷰 신고 처리 중 예외 발생", e);
            return ResponseEntity.status(500).body("신고 처리 오류 발생");
        }
    }

    // // 관리자: 신고된 리뷰 리스트 조회
    // @PreAuthorize("hasAuthority('ADMIN')")
    // @GetMapping("/reports")
    // public ResponseEntity<List<ReviewReportDto>> getAllReports() {
  
    //     List<ReviewReportDto> reports = reviewReportService.getAllReports();
    //     log.info("관리자 신고 리뷰 리스트 조회 완료: count={}", reports.size());
        
    //     return ResponseEntity.ok(reports);

    // }

    // 관리자: 신고된 리뷰 검색
    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<ReviewReportDto>> searchReports(
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        Page<ReviewReportDto> result = reviewReportService.searchReports(type, from, to, keyword, pageable);
        log.info("result: {}", result);
        return ResponseEntity.ok(result);
    }


    // 관리자: 신고(리포트) 여러 건 삭제
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/report")
    public ResponseEntity<?> deleteReports(@RequestBody List<Long> reportIds) {
        log.info("관리자 신고 삭제 요청: reportIds={}", reportIds);
        try {
            reviewReportService.deleteReports(reportIds);
            log.info("관리자 신고 삭제 완료: reportIds={}", reportIds);
            return ResponseEntity.ok("신고가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("신고 삭제 실패(잘못된 요청): {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("신고 삭제 처리 중 예외 발생", e);
            return ResponseEntity.status(500).body("신고 삭제 처리 오류 발생");
        }
    }

    // 관리자: 특정 리뷰의 신고 리스트 조회
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/reports/{reviewId}")
    public ResponseEntity<List<ReviewReportDto>> getReportsByReviewId(@PathVariable Long reviewId) {
        List<ReviewReportDto> reports = reviewReportService.getReportsByReviewId(reviewId);
        return ResponseEntity.ok(reports);
    }
}
