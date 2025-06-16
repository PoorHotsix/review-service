package com.inkcloud.review_service.repository;

import com.inkcloud.review_service.domain.QReviewReport;
import com.inkcloud.review_service.domain.ReportType;
import com.inkcloud.review_service.domain.ReviewReport;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ReviewReportRepositoryImpl implements ReviewReportRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ReviewReport> searchReports(
            ReportType type,
            LocalDateTime from,
            LocalDateTime to,
            String keyword,
            Pageable pageable
    ) {
        QReviewReport report = QReviewReport.reviewReport;

        BooleanExpression predicate = report.id.isNotNull();

        if (type != null) {
            predicate = predicate.and(report.type.eq(type));
        }
        if (from != null && to != null) {
            predicate = predicate.and(report.reportedAt.between(from, to));
        }
        if (keyword != null && !keyword.isBlank()) {
            predicate = predicate.and(
                report.reason.containsIgnoreCase(keyword)
                .or(report.reporterEmail.containsIgnoreCase(keyword))
            );
        }

        List<ReviewReport> content = queryFactory
                .selectFrom(report)
                .where(predicate)
                .orderBy(report.reportedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(report.count())
                .from(report)
                .where(predicate)
                .fetchOne();

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }
}
