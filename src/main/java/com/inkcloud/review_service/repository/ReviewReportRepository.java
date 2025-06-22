package com.inkcloud.review_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inkcloud.review_service.domain.ReviewReport;

import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long>, ReviewReportRepositoryCustom {

    List<ReviewReport> findAllByOrderByReportedAtDesc();

    boolean existsByReviewIdAndReporterEmail(Long reviewId, String reporterEmail);

    List<ReviewReport> findAllByReviewId(Long reviewId);
}
