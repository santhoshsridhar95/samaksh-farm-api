package com.samaksh.farms.review.repo;

import com.samaksh.farms.review.entity.CustomerReview;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerReviewRepository
        extends JpaRepository<CustomerReview, Long> {

    List<CustomerReview> findByPublishedTrue(
            Sort sort
    );

    Optional<CustomerReview> findFirstByNameIgnoreCaseAndReviewIgnoreCaseOrderByCreatedAtDesc(
            String name,
            String review
    );
}
