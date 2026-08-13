package com.samaksh.farms.review.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.review.dto.CustomerReviewRequest;
import com.samaksh.farms.review.dto.CustomerReviewResponse;
import com.samaksh.farms.review.entity.CustomerReview;
import com.samaksh.farms.review.repo.CustomerReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerReviewService {

    private final CustomerReviewRepository reviewRepository;

    private final AuditService auditService;

    public List<CustomerReviewResponse> getPublishedReviews() {
        return reviewRepository.findByPublishedTrue(
                        Sort.by("createdAt").descending()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CustomerReviewResponse> getAllReviews() {
        return reviewRepository.findAll(
                        Sort.by("createdAt").descending()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CustomerReviewResponse createReview(
            CustomerReviewRequest request
    ) {

        String name = clean(request.getName());
        String location = defaultLocation(request.getLocation());
        String reviewText = clean(request.getReview());

        CustomerReview existingReview =
                reviewRepository.findFirstByNameIgnoreCaseAndReviewIgnoreCaseOrderByCreatedAtDesc(
                                name,
                                reviewText
                        )
                        .orElse(null);

        if (existingReview != null) {
            existingReview.setLocation(location);
            existingReview.setRating(request.getRating());
            existingReview.setPublished(true);
            existingReview.setUpdatedAt(LocalDateTime.now());

            return mapToResponse(reviewRepository.save(existingReview));
        }

        CustomerReview review =
                CustomerReview.builder()
                        .name(name)
                        .location(location)
                        .review(reviewText)
                        .rating(request.getRating())
                        .published(true)
                        .createdAt(LocalDateTime.now())
                        .build();

        return mapToResponse(reviewRepository.save(review));
    }

    public CustomerReviewResponse updateReview(
            Long id,
            CustomerReviewRequest request,
            Authentication authentication
    ) {

        CustomerReview review =
                reviewRepository.findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Customer review",
                                        id
                                )
                        );

        review.setName(clean(request.getName()));
        review.setLocation(defaultLocation(request.getLocation()));
        review.setReview(clean(request.getReview()));
        review.setRating(request.getRating());
        review.setPublished(
                request.getPublished() == null
                        ? true
                        : request.getPublished()
        );
        review.setUpdatedAt(LocalDateTime.now());

        CustomerReview savedReview =
                reviewRepository.save(review);

        auditService.createAudit(
                authentication,
                "REVIEW",
                "UPDATE_REVIEW",
                String.valueOf(savedReview.getId()),
                "Customer review updated"
        );

        return mapToResponse(savedReview);
    }

    public CustomerReviewResponse deleteReview(
            Long id,
            Authentication authentication
    ) {

        CustomerReview review =
                reviewRepository.findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Customer review",
                                        id
                                )
                        );

        reviewRepository.delete(review);

        auditService.createAudit(
                authentication,
                "REVIEW",
                "DELETE_REVIEW",
                String.valueOf(id),
                "Customer review deleted"
        );

        return mapToResponse(review);
    }

    private CustomerReviewResponse mapToResponse(
            CustomerReview review
    ) {

        return CustomerReviewResponse.builder()
                .id(review.getId())
                .name(review.getName())
                .location(review.getLocation())
                .review(review.getReview())
                .rating(review.getRating())
                .published(review.getPublished())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    private String defaultLocation(
            String value
    ) {

        String cleanValue = clean(value);

        return cleanValue.isBlank()
                ? "Bengaluru"
                : cleanValue;
    }
}
