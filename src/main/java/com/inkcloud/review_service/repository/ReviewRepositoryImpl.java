package com.inkcloud.review_service.repository;

import com.inkcloud.review_service.domain.QReview;
import com.inkcloud.review_service.domain.Review;
import com.inkcloud.review_service.dto.ReviewDto;
import com.inkcloud.review_service.util.ReviewMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final ReviewMapper reviewMapper; // 서비스 주입

    @Override
    public Page<ReviewDto> searchReviews(
            String keyword, String startDate, String endDate, Integer minRating, Integer maxRating, Pageable pageable) {

        QReview review = QReview.review;
        BooleanBuilder builder = new BooleanBuilder();

        if (keyword != null && !keyword.isEmpty()) {
            builder.and(
                review.productName.containsIgnoreCase(keyword)
                    .or(review.comment.containsIgnoreCase(keyword))
                    .or(review.email.containsIgnoreCase(keyword)) // 이메일도 검색
            );
        }
        if (startDate != null && endDate != null) {
            LocalDateTime start;
            LocalDateTime end;

            if (startDate.endsWith("Z")) {
                // Zulu(UTC) 포맷 처리
                DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                start = OffsetDateTime.parse(startDate, formatter).toLocalDateTime();
                end = OffsetDateTime.parse(endDate, formatter).toLocalDateTime();
            } else {
                // 기존 방식 (yyyy-MM-ddTHH:mm:ss 등)
                start = LocalDateTime.parse(startDate);
                end = LocalDateTime.parse(endDate);
            }

            builder.and(review.createdAt.between(start, end));
        }
        if (minRating != null) {
            builder.and(review.rating.goe(minRating));
        }
        if (maxRating != null) {
            builder.and(review.rating.loe(maxRating));
        }

        List<Review> content = queryFactory
                .selectFrom(review)
                .where(builder)
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(review)
                .where(builder)
                .fetchCount();

        List<ReviewDto> dtoList = content.stream()
            .map(reviewMapper::entityToDto)
            .toList();

        return new PageImpl<>(dtoList, pageable, total);
    }
}
