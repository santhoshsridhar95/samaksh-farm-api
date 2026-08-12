package com.samaksh.farms.review.repo;

import com.samaksh.farms.review.entity.CustomerReview;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerReviewRepository
        extends JpaRepository<CustomerReview, Long> {

    List<CustomerReview> findByPublishedTrue(
            Sort sort
    );
}
