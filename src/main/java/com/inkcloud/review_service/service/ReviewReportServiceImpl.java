package com.inkcloud.review_service.service;

import com.inkcloud.review_service.domain.ReportType;
import com.inkcloud.review_service.domain.Review;
import com.inkcloud.review_service.domain.ReviewReport;
import com.inkcloud.review_service.dto.ReviewReportDto;

import com.inkcloud.review_service.repository.ReviewReportRepository;
import com.inkcloud.review_service.repository.ReviewRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@ToString
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewReportServiceImpl implements ReviewReportService {
    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;

    //리뷰 신고
    @Override
    public void reportReview(Long reviewId, String reporterEmail, ReportType type, String reason) {
        log.info("리뷰 신고 요청: reviewId={}, reporterEmail={}, type={}, reason={}", reviewId, reporterEmail, type, reason);
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

            //중복 신고 방지 
        if (reviewReportRepository.existsByReviewIdAndReporterEmail(reviewId, reporterEmail)) {
        throw new IllegalArgumentException("이미 신고한 리뷰입니다.");
        }

        ReviewReport report = ReviewReport.builder()
            .review(review)
            .reporterEmail(reporterEmail)
            .type(type)
            .reason(reason)
            .reportedAt(LocalDateTime.now())
            .build();
        reviewReportRepository.save(report);
        log.info("리뷰 신고 저장 완료");
    }

    // @Override
    // public List<ReviewReportDto> getAllReports() {
    //     List<ReviewReport> reports = reviewReportRepository.findAllByOrderByReportedAtDesc();
    //     return reports.stream()
    //             .map(this::entityToDto)
    //             .toList();
    // }

    //관리자 리뷰리포트 조회
    @Override
    public Page<ReviewReportDto> searchReports(ReportType type, LocalDateTime from, LocalDateTime to, String keyword, Pageable pageable) {
        Page<ReviewReport> reports = reviewReportRepository.searchReports(type, from, to, keyword, pageable);

        Page<ReviewReportDto> dtoPage = reports.map(this::entityToDto);

        // 엔티티의 reportedAt 로그로 출력
        for (ReviewReport report : reports.getContent()) {
            log.info("리뷰리포트 reportedAt: {}", report.getReportedAt());
        }

        // DTO의 reportedAt 로그로 출력
        for (ReviewReportDto dto : dtoPage.getContent()) {
            log.info("리뷰리포트 DTO reportedAt: {}", dto.getReportedAt());
        }

        return dtoPage;
    }

    // DTO에 productId, productName 추가 (Review 엔티티에서 바로 조회)
    public ReviewReportDto entityToDto(ReviewReport entity) {
        Review review = reviewRepository.findById(entity.getReview().getId())
            .orElse(null);

        Long productId = null;
        String productName = null;
        if (review != null) {
            productId = review.getProductId();
            productName = review.getProductName();
        }

        return ReviewReportDto.builder()
                .id(entity.getId())
                .reviewId(entity.getReview().getId())
                .productId(productId)
                .productName(productName)
                .reporterEmail(entity.getReporterEmail())
                .type(entity.getType()) 
                .reason(entity.getReason())
                .reportedAt(entity.getReportedAt())
                .build();
    }

    //리뷰 신고 내역 삭제 
    @Override
    public void deleteReports(List<Long> reportIds) {
        log.info("리뷰 신고 여러 건 삭제 요청: reportIds={}", reportIds);
        for (Long reportId : reportIds) {
            ReviewReport report = reviewReportRepository.findById(reportId)
                    .orElseThrow(() -> new IllegalArgumentException("신고 내역을 찾을 수 없습니다. (reportId=" + reportId + ")"));
            reviewReportRepository.delete(report);
            log.info("리뷰 신고 삭제 완료: reportId={}", reportId);
        }
    }

    @Override
    public List<ReviewReportDto> getReportsByReviewId(Long reviewId) {
        List<ReviewReport> reports = reviewReportRepository.findAllByReviewId(reviewId);
        return reports.stream()
                .map(this::entityToDto)
                .toList();
    }
}
