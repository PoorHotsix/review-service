package com.inkcloud.review_service.dto;

import java.time.LocalDateTime;

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
public class ReviewLikeDto {

    private Long id;

    private Long reviewId;

    private String email; // 좋아요 누른 사용자 이메일

    private LocalDateTime createdAt;
    
}
