package com.inkcloud.review_service.repository;

import com.inkcloud.review_service.domain.ReportType;
import com.inkcloud.review_service.domain.ReviewReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ReviewReportRepositoryCustom {
    Page<ReviewReport> searchReports(
        ReportType type,
        LocalDateTime from,
        LocalDateTime to,
        String keyword,
        Pageable pageable
    );
}
