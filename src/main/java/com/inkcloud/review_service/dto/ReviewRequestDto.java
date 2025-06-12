package com.inkcloud.review_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDto {
    private int page = 0;
    private int size = 20;
    private String keyword;
    private String startDate;
    private String endDate;
    private Integer minRating;
    private Integer maxRating;
}
