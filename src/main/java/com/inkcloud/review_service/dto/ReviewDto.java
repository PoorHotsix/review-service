package com.inkcloud.review_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {

    private Long id;

    private String email;

    private Long productId;

    private String productName;

    private Integer rating;

    private String comment;

    private int likeCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean likedByMe;
    
}
