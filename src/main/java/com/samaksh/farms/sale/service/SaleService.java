package com.samaksh.farms.sale.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.enums.ExchangeType;
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

        double quantity =
                request.getQuantity() == null
                        ? 0
                        : request.getQuantity();

        double unitPrice =
                request.getUnitPrice() == null
                        ? 0
                        : request.getUnitPrice();

        double totalAmount =
                quantity * unitPrice;

        double amountCollected =
                request.getAmountCollected() == null
                        ? 0
                        : request.getAmountCollected();

        double pendingAmount =
                totalAmount - amountCollected;

        PaymentStatus paymentStatus =
                request.getPaymentStatus() == null
                        ? resolvePaymentStatus(
                                totalAmount,
                                amountCollected
                        )
                        : request.getPaymentStatus();

        Sale sale =
                Sale.builder()
                        .customer(customer)
                        .product(product)
                        .quantity(
                                quantity
                        )
                        .unitPrice(
                                unitPrice
                        )
                        .totalAmount(
                                totalAmount
                        )
                        .amountCollected(
                                amountCollected
                        )
                        .pendingAmount(
                                pendingAmount
                        )
                        .shopkeeperSellingPrice(
                                request.getShopkeeperSellingPrice()
                        )
                        .exchangeType(
                                request.getExchangeType() == null
                                        ? ExchangeType.NONE
                                        : request.getExchangeType()
                        )
                        .exchangeBoxes(
                                request.getExchangeBoxes() == null
                                        ? 0
                                        : request.getExchangeBoxes()
                        )
                        .returnedBoxes(
                                request.getReturnedBoxes() == null
                                        ? 0
                                        : request.getReturnedBoxes()
                        )
                        .paymentStatus(
                                paymentStatus
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

    private PaymentStatus resolvePaymentStatus(
            double totalAmount,
            double amountCollected
    ) {

        if (amountCollected >= totalAmount &&
                totalAmount > 0) {

            return PaymentStatus.PAID;
        }

        if (amountCollected > 0) {

            return PaymentStatus.PARTIAL;
        }

        return PaymentStatus.PENDING;
    }

    private SaleResponse mapToResponse(
            Sale sale
    ) {

        return SaleResponse.builder()
                .id(
                        sale.getId()
                )
                .customerId(
                        sale.getCustomer()
                                .getId()
                )
                .customerName(
                        sale.getCustomer()
                                .getCustomerName()
                )
                .shopCategory(
                        sale.getCustomer()
                                .getShopCategory()
                )
                .location(
                        sale.getCustomer()
                                .getLocation() == null
                                ? "R.T. Nagar"
                                : sale.getCustomer()
                                .getLocation()
                )
                .minimumBoxesPerDay(
                        sale.getCustomer()
                                .getMinimumBoxesPerDay()
                )
                .productId(
                        sale.getProduct()
                                .getId()
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
                .amountCollected(
                        sale.getAmountCollected()
                )
                .pendingAmount(
                        sale.getPendingAmount()
                )
                .shopkeeperSellingPrice(
                        sale.getShopkeeperSellingPrice()
                )
                .exchangeType(
                        sale.getExchangeType()
                )
                .exchangeBoxes(
                        sale.getExchangeBoxes()
                )
                .returnedBoxes(
                        sale.getReturnedBoxes()
                )
                .paymentStatus(
                        sale.getPaymentStatus()
                )
                .remarks(
                        sale.getRemarks()
                )
                .saleDate(
                        sale.getSaleDate()
                )
                .build();
    }
}
