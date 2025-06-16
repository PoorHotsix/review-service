package com.inkcloud.review_service.repository;

import com.inkcloud.review_service.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

    // 책 ID로 리뷰 조회
    List<Review> findAllByProductId(Long productId);

    // 리뷰 작성시 상품에 이미 작성한 회원의 리뷰가 있는지 확인
    Optional<Review> findByProductIdAndEmail(Long productId, String email);

    // 회원이메일로 리뷰 조회+기간별
    List<Review> findByEmailAndCreatedAtBetween(String email, LocalDateTime start, LocalDateTime end);
}
