package com.inkcloud.review_service.dto;

import java.time.LocalDateTime;

import com.inkcloud.review_service.domain.ReportType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReportDto {
    private Long id;
    private Long reviewId;
    private String reporterEmail;
    private ReportType type; 
    private String reason;
    private LocalDateTime reportedAt;
}
