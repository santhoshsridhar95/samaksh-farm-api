package com.samaksh.farms.products.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.products.dto.ProductRequest;
import com.samaksh.farms.products.dto.ProductResponse;
import com.samaksh.farms.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(
            @RequestBody ProductRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<ProductResponse>builder()
                .success(true)
                .message(
                        "Product created successfully"
                )
                .data(
                        productService.createProduct(
                                request,
                                authentication
                        )
                )
                .build();
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>>
    getProducts() {

        return ApiResponse
                .<List<ProductResponse>>builder()
                .success(true)
                .message(
                        "Products fetched successfully"
                )
                .data(
                        productService.getProducts()
                )
                .build();
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<ProductResponse>
    disableProduct(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ApiResponse
                .<ProductResponse>builder()
                .success(true)
                .message(
                        "Product disabled successfully"
                )
                .data(
                        productService.disableProduct(
                                id,
                                authentication
                        )
                )
                .build();
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<ProductResponse>
    enableProduct(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ApiResponse
                .<ProductResponse>builder()
                .success(true)
                .message(
                        "Product enabled successfully"
                )
                .data(
                        productService.enableProduct(
                                id,
                                authentication
                        )
                )
                .build();
    }
}