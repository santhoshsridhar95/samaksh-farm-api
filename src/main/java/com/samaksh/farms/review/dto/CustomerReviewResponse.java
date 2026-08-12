package com.samaksh.farms.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CustomerReviewResponse {

    private Long id;

    private String name;

    private String location;

    private String review;

    private Integer rating;

    private Boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
