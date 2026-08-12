package com.samaksh.farms.review.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.review.dto.CustomerReviewRequest;
import com.samaksh.farms.review.dto.CustomerReviewResponse;
import com.samaksh.farms.review.service.CustomerReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class CustomerReviewController {

    private final CustomerReviewService reviewService;

    @GetMapping
    public ApiResponse<List<CustomerReviewResponse>> getPublishedReviews() {
        return ApiResponse
                .<List<CustomerReviewResponse>>builder()
                .success(true)
                .message("Reviews fetched successfully")
                .data(reviewService.getPublishedReviews())
                .build();
    }

    @PostMapping
    public ApiResponse<CustomerReviewResponse> createReview(
            @Valid @RequestBody CustomerReviewRequest request
    ) {
        return ApiResponse
                .<CustomerReviewResponse>builder()
                .success(true)
                .message("Review published successfully")
                .data(reviewService.createReview(request))
                .build();
    }

    @GetMapping("/admin")
    public ApiResponse<List<CustomerReviewResponse>> getAllReviews() {
        return ApiResponse
                .<List<CustomerReviewResponse>>builder()
                .success(true)
                .message("Reviews fetched successfully")
                .data(reviewService.getAllReviews())
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody CustomerReviewRequest request,
            Authentication authentication
    ) {
        return ApiResponse
                .<CustomerReviewResponse>builder()
                .success(true)
                .message("Review updated successfully")
                .data(reviewService.updateReview(id, request, authentication))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<CustomerReviewResponse> deleteReview(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse
                .<CustomerReviewResponse>builder()
                .success(true)
                .message("Review deleted successfully")
                .data(reviewService.deleteReview(id, authentication))
                .build();
    }
}
