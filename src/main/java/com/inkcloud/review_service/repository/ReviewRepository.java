package com.inkcloud.review_service.repository;

import com.inkcloud.review_service.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    // 책 ID로 리뷰 조회
    List<Review> findAllByProductId(String productId);

    // 회원 이메일로 리뷰 조회
    List<Review> findAllByEmail(String email);
}
