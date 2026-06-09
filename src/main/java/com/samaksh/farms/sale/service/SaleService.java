package com.samaksh.farms.sale.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.enums.PaymentStatus;
import com.samaksh.farms.products.entity.Product;
import com.samaksh.farms.products.repo.ProductRepository;
import com.samaksh.farms.sale.dto.PagedResponse;
import com.samaksh.farms.sale.dto.SaleRequest;
import com.samaksh.farms.sale.dto.SaleResponse;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;

    private final CustomerRepository customerRepository;

    private final ProductRepository productRepository;

    private final AuditService auditService;

    public SaleResponse createSale(
            SaleRequest request,
            Authentication authentication
    ) {

        Customer customer =
                customerRepository.findById(
                        request.getCustomerId()
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Customer",
                                request.getCustomerId()
                        )
                );

        Product product =
                productRepository.findById(
                        request.getProductId()
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Product",
                                request.getProductId()
                        )
                );

        double totalAmount =
                request.getQuantity()
                        * request.getUnitPrice();

        Sale sale =
                Sale.builder()
                        .customer(customer)
                        .product(product)
                        .quantity(
                                request.getQuantity()
                        )
                        .unitPrice(
                                request.getUnitPrice()
                        )
                        .totalAmount(
                                totalAmount
                        )
                        .paymentStatus(
                                request.getPaymentStatus()
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .saleDate(
                                LocalDateTime.now()
                        )
                        .build();

        Sale savedSale =
                saleRepository.save(
                        sale
                );

        auditService.createAudit(
                authentication,
                "SALE",
                "CREATE_SALE",
                savedSale.getId().toString(),
                "Sale Amount : "
                        + totalAmount
        );

        return mapToResponse(
                savedSale
        );
    }

    public PagedResponse<SaleResponse> getSales(
            int page,
            int size,
            Long customerId,
            PaymentStatus paymentStatus
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("saleDate")
                                .descending()
                );

        Page<Sale> sales;

        if (customerId != null &&
                paymentStatus != null) {

            sales =
                    saleRepository
                            .findByCustomerIdAndPaymentStatus(
                                    customerId,
                                    paymentStatus,
                                    pageable
                            );

        } else if (customerId != null) {

            sales =
                    saleRepository.findByCustomerId(
                            customerId,
                            pageable
                    );

        } else if (paymentStatus != null) {

            sales =
                    saleRepository.findByPaymentStatus(
                            paymentStatus,
                            pageable
                    );

        } else {

            sales =
                    saleRepository.findAll(
                            pageable
                    );
        }

        return PagedResponse
                .<SaleResponse>builder()
                .content(
                        sales.getContent()
                                .stream()
                                .map(this::mapToResponse)
                                .toList()
                )
                .page(
                        sales.getNumber()
                )
                .size(
                        sales.getSize()
                )
                .totalElements(
                        sales.getTotalElements()
                )
                .totalPages(
                        sales.getTotalPages()
                )
                .last(
                        sales.isLast()
                )
                .build();
    }

    private SaleResponse mapToResponse(
            Sale sale
    ) {

        return SaleResponse.builder()
                .id(
                        sale.getId()
                )
                .customerName(
                        sale.getCustomer()
                                .getCustomerName()
                )
                .productName(
                        sale.getProduct()
                                .getProductName()
                )
                .quantity(
                        sale.getQuantity()
                )
                .unitPrice(
                        sale.getUnitPrice()
                )
                .totalAmount(
                        sale.getTotalAmount()
                )
                .paymentStatus(
                        sale.getPaymentStatus()
                )
                .remarks(
                        sale.getRemarks()
                )
                .build();
    }
}