package com.inkcloud.review_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEventDto {
    private String type;      // "created", "updated", "deleted"
    private Long productId;
    private Integer rating;
    private Integer oldRating;
}
