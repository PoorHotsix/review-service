package com.inkcloud.review_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.inkcloud.review_service.domain.ReportType;
import com.inkcloud.review_service.domain.ReviewReport;
import com.inkcloud.review_service.dto.ReviewReportDto;

public interface ReviewReportService {

    //리뷰 신고하기
    void reportReview(Long reviewId, String reporterEmail, ReportType type,  String reason);

    //관리자 리뷰 리포트 조회 
    // List<ReviewReportDto> getAllReports(); // 관리자용
    Page<ReviewReportDto> searchReports(ReportType type, LocalDateTime from, LocalDateTime to, String keyword, Pageable pageable);

    // 리포트(신고) 삭제
    void deleteReports(List<Long> reportIds);
    
    ReviewReportDto entityToDto(ReviewReport entity);

    List<ReviewReportDto> getReportsByReviewId(Long reviewId);
}
