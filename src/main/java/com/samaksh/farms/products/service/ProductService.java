package com.samaksh.farms.products.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.enums.ProductUnitType;
import com.samaksh.farms.products.dto.ProductRequest;
import com.samaksh.farms.products.dto.ProductResponse;
import com.samaksh.farms.products.entity.Product;
import com.samaksh.farms.products.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final AuditService auditService;

    public ProductResponse createProduct(
            ProductRequest request,
            Authentication authentication
    ) {

        validateProductForCreate(request);

        Product product =
                Product.builder()
                        .productCode(
                                generateProductCode()
                        )
                        .productName(
                                cleaned(request.getProductName())
                        )
                        .unitType(
                                request.getUnitType() == null
                                        ? ProductUnitType.KG
                                        : request.getUnitType()
                        )
                        .standardPrice(
                                request.getStandardPrice() == null
                                        ? 0D
                                        : request.getStandardPrice()
                        )
                        .active(true)
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        Product savedProduct =
                productRepository.save(product);

        auditService.createAudit(
                authentication,
                "PRODUCT",
                "CREATE_PRODUCT",
                savedProduct.getProductCode(),
                "Product Created"
        );

        return mapToResponse(savedProduct);
    }

    public List<ProductResponse> getProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse updateProduct(
            Long productId,
            ProductRequest request,
            Authentication authentication
    ) {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product",
                                                productId
                                        )
                        );

        applyProductUpdate(product, request);

        Product savedProduct =
                productRepository.save(product);

        auditService.createAudit(
                authentication,
                "PRODUCT",
                "UPDATE_PRODUCT",
                savedProduct.getProductCode(),
                "Product Updated"
        );

        return mapToResponse(savedProduct);
    }

    public ProductResponse disableProduct(
            Long productId,
            Authentication authentication
    ) {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product",
                                                productId
                                        )
                        );

        product.setActive(false);

        Product savedProduct =
                productRepository.save(product);

        auditService.createAudit(
                authentication,
                "PRODUCT",
                "DISABLE_PRODUCT",
                savedProduct.getProductCode(),
                "Product Disabled"
        );

        return mapToResponse(savedProduct);
    }

    public ProductResponse enableProduct(
            Long productId,
            Authentication authentication
    ) {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product",
                                                productId
                                        )
                        );

        product.setActive(true);

        Product savedProduct =
                productRepository.save(product);

        auditService.createAudit(
                authentication,
                "PRODUCT",
                "ENABLE_PRODUCT",
                savedProduct.getProductCode(),
                "Product Enabled"
        );

        return mapToResponse(savedProduct);
    }

    private ProductResponse mapToResponse(
            Product product
    ) {

        return ProductResponse.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .unitType(product.getUnitType())
                .standardPrice(product.getStandardPrice())
                .active(product.getActive())
                .build();
    }

    private void validateProductForCreate(ProductRequest request) {

        if (request == null || cleaned(request.getProductName()).isBlank()) {
            throw new RuntimeException("Product name is required");
        }

        validatePrice(request.getStandardPrice());
    }

    private void applyProductUpdate(
            Product product,
            ProductRequest request
    ) {

        if (request == null) {
            return;
        }

        String productName =
                cleaned(request.getProductName());

        if (!productName.isBlank()) {
            product.setProductName(productName);
        }

        if (request.getUnitType() != null) {
            product.setUnitType(request.getUnitType());
        }

        if (request.getStandardPrice() != null) {
            validatePrice(request.getStandardPrice());
            product.setStandardPrice(request.getStandardPrice());
        }
    }

    private void validatePrice(Double price) {

        if (price != null && price < 0) {
            throw new RuntimeException("Product price cannot be negative");
        }
    }

    private String cleaned(String value) {

        return value == null
                ? ""
                : value.trim();
    }

    private String generateProductCode() {

        long count =
                productRepository.count() + 1;

        return "SF-PROD-"
                + String.format(
                "%03d",
                count
        );
    }
}
