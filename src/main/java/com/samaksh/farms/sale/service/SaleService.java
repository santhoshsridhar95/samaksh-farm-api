package com.samaksh.farms.sale.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.enums.ExchangeType;
import com.samaksh.farms.enums.PaymentStatus;
import com.samaksh.farms.enums.ProductUnitType;
import com.samaksh.farms.products.entity.Product;
import com.samaksh.farms.products.repo.ProductRepository;
import com.samaksh.farms.sale.dto.PagedResponse;
import com.samaksh.farms.sale.dto.SalePaymentUpdateRequest;
import com.samaksh.farms.sale.dto.SaleRequest;
import com.samaksh.farms.sale.dto.SaleResponse;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;

    private final CustomerRepository customerRepository;

    private final ProductRepository productRepository;

    private final AuditService auditService;

    private final UserRepository userRepository;

    public SaleResponse createSale(
            SaleRequest request,
            Authentication authentication
    ) {

        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException(
                    "Shop is required"
            );
        }

        Customer customer =
                customerRepository.findById(
                        request.getCustomerId()
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Customer",
                                request.getCustomerId()
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

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Boxes must be greater than 0"
            );
        }

        if (unitPrice < 0) {
            throw new IllegalArgumentException(
                    "Unit price cannot be negative"
            );
        }

        Product product =
                resolveProduct(
                        request,
                        unitPrice
                );

        double totalAmount =
                quantity * unitPrice;

        ExchangeType exchangeType =
                request.getExchangeType() == null
                        ? ExchangeType.NONE
                        : request.getExchangeType();

        double exchangeBoxes =
                request.getExchangeBoxes() == null
                        ? 0
                        : request.getExchangeBoxes();

        if (exchangeBoxes < 0) {
            throw new IllegalArgumentException(
                    "Exchange boxes cannot be negative"
            );
        }

        double returnedBoxes =
                request.getReturnedBoxes() == null
                        ? 0
                        : request.getReturnedBoxes();

        if (returnedBoxes < 0) {
            throw new IllegalArgumentException(
                    "Returned boxes cannot be negative"
            );
        }

        Double shopkeeperSellingPrice =
                request.getShopkeeperSellingPrice();

        if (shopkeeperSellingPrice != null &&
                shopkeeperSellingPrice < 0) {
            throw new IllegalArgumentException(
                    "Shopkeeper selling price cannot be negative"
            );
        }

        double exchangeCredit =
                exchangeCredit(
                        exchangeType,
                        exchangeBoxes,
                        unitPrice
                );

        double billableAmount =
                Math.max(
                        0,
                        totalAmount - exchangeCredit
                );

        double receivedToday =
                request.getAmountCollected() == null
                        ? 0
                        : request.getAmountCollected();

        if (receivedToday < 0) {
            throw new IllegalArgumentException(
                    "Collected amount cannot be negative"
            );
        }

        double outstandingBeforeSale =
                customerPendingBalance(
                        customer.getId()
                );

        if (receivedToday > outstandingBeforeSale + billableAmount) {
            throw new IllegalArgumentException(
                    "Collected amount cannot be greater than shop outstanding plus today's bill"
            );
        }

        CashCollector cashCollector =
                resolveCashCollector(
                        request,
                        authentication
                );

        double remainingCollection =
                applyCollectionToPreviousSales(
                        customer.getId(),
                        receivedToday,
                        authentication,
                        cashCollector
                );

        double amountAppliedToCurrentSale =
                Math.min(
                        billableAmount,
                        remainingCollection
                );

        double pendingAmount =
                billableAmount - amountAppliedToCurrentSale;

        PaymentStatus paymentStatus =
                resolvePaymentStatus(
                        billableAmount,
                        amountAppliedToCurrentSale
                );

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
                                billableAmount
                        )
                        .amountCollected(
                                amountAppliedToCurrentSale
                        )
                        .pendingAmount(
                                pendingAmount
                        )
                        .shopkeeperSellingPrice(
                                shopkeeperSellingPrice
                        )
                        .exchangeType(
                                exchangeType
                        )
                        .exchangeBoxes(
                                exchangeBoxes
                        )
                        .returnedBoxes(
                                returnedBoxes
                        )
                        .paymentStatus(
                                paymentStatus
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .createdByUserId(
                                currentUserId(
                                        authentication
                                )
                        )
                        .createdByName(
                                currentUserName(
                                        authentication
                                )
                        )
                        .createdByEmail(
                                currentUserEmail(
                                        authentication
                                )
                        )
                        .collectorUserId(
                                cashCollector.userId()
                        )
                        .collectorName(
                                cashCollector.name()
                        )
                        .collectorEmail(
                                cashCollector.email()
                        )
                        .saleDate(
                                BusinessTime.now()
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
                        + billableAmount
                        + ", received today Rs. "
                        + receivedToday
                        + ", applied to this sale Rs. "
                        + amountAppliedToCurrentSale
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

    public SaleResponse updatePayment(
            Long saleId,
            SalePaymentUpdateRequest request,
            Authentication authentication
    ) {

        Sale sale =
                saleRepository.findById(
                        saleId
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Sale",
                                saleId
                        )
                );

        double totalAmount =
                saleTotal(
                        sale
                );

        double newAmountCollected =
                request.getAmountCollected() == null
                        ? saleCollected(
                                sale
                        )
                        : request.getAmountCollected();

        if (newAmountCollected < 0) {
            throw new IllegalArgumentException(
                    "Collected amount cannot be negative"
            );
        }

        if (newAmountCollected > totalAmount) {
            throw new IllegalArgumentException(
                    "Collected amount cannot be greater than total amount"
            );
        }

        double oldAmountCollected =
                saleCollected(
                        sale
                );

        double oldPendingAmount =
                salePending(
                        sale
                );

        PaymentStatus oldPaymentStatus =
                sale.getPaymentStatus();

        double newPendingAmount =
                Math.max(
                        0,
                        totalAmount - newAmountCollected
                );

        PaymentStatus newPaymentStatus =
                resolvePaymentStatus(
                        totalAmount,
                        newAmountCollected
                );

        sale.setAmountCollected(
                newAmountCollected
        );
        sale.setPendingAmount(
                newPendingAmount
        );
        sale.setPaymentStatus(
                newPaymentStatus
        );
        applyUpdatedBy(
                sale,
                authentication
        );

        if (request.getRemarks() != null &&
                !request.getRemarks().isBlank()) {

            String existingRemarks =
                    sale.getRemarks() == null
                            ? ""
                            : sale.getRemarks();

            sale.setRemarks(
                    existingRemarks.isBlank()
                            ? request.getRemarks()
                            : existingRemarks + " | Payment: "
                                    + request.getRemarks()
            );
        }

        Sale savedSale =
                saleRepository.save(
                        sale
                );

        auditService.createAudit(
                authentication,
                "SALE",
                "UPDATE_PAYMENT",
                savedSale.getId().toString(),
                "Payment changed for "
                        + savedSale.getCustomer().getCustomerName()
                        + ": collected Rs. "
                        + oldAmountCollected
                        + " -> Rs. "
                        + newAmountCollected
                        + ", pending Rs. "
                        + oldPendingAmount
                        + " -> Rs. "
                        + newPendingAmount
                        + ", status "
                        + oldPaymentStatus
                        + " -> "
                        + newPaymentStatus
        );

        return mapToResponse(
                savedSale
        );
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

    private double applyCollectionToPreviousSales(
            Long customerId,
            double collectionAmount,
            Authentication authentication,
            CashCollector cashCollector
    ) {

        if (collectionAmount <= 0) {
            return 0;
        }

        double remainingCollection =
                collectionAmount;

        List<Sale> unpaidSales =
                saleRepository.findByCustomerIdAndPaymentStatusInOrderBySaleDateAsc(
                        customerId,
                        List.of(
                                PaymentStatus.PENDING,
                                PaymentStatus.PARTIAL
                        )
                );

        for (Sale unpaidSale : unpaidSales) {

            if (remainingCollection <= 0) {
                break;
            }

            double pendingAmount =
                    salePending(
                            unpaidSale
                    );

            if (pendingAmount <= 0) {
                continue;
            }

            double appliedAmount =
                    Math.min(
                            pendingAmount,
                            remainingCollection
                    );

            double oldCollected =
                    saleCollected(
                            unpaidSale
                    );

            double oldPending =
                    pendingAmount;

            PaymentStatus oldStatus =
                    unpaidSale.getPaymentStatus();

            double newCollected =
                    oldCollected + appliedAmount;

            double newPending =
                    Math.max(
                            0,
                            saleTotal(
                                    unpaidSale
                            ) - newCollected
                    );

            unpaidSale.setAmountCollected(
                    newCollected
            );
            unpaidSale.setPendingAmount(
                    newPending
            );
            unpaidSale.setPaymentStatus(
                    resolvePaymentStatus(
                            saleTotal(
                                    unpaidSale
                            ),
                            newCollected
                    )
            );
            unpaidSale.setCollectorUserId(
                    cashCollector.userId()
            );
            unpaidSale.setCollectorName(
                    cashCollector.name()
            );
            unpaidSale.setCollectorEmail(
                    cashCollector.email()
            );
            applyUpdatedBy(
                    unpaidSale,
                    authentication
            );

            Sale savedSale =
                    saleRepository.save(
                            unpaidSale
                    );

            auditService.createAudit(
                    authentication,
                    "SALE",
                    "APPLY_DELIVERY_COLLECTION",
                    savedSale.getId().toString(),
                    "Delivery collection applied to previous sale: Rs. "
                            + appliedAmount
                            + ", collected Rs. "
                            + oldCollected
                            + " -> Rs. "
                            + newCollected
                            + ", pending Rs. "
                            + oldPending
                            + " -> Rs. "
                            + newPending
                            + ", status "
                            + oldStatus
                            + " -> "
                            + savedSale.getPaymentStatus()
            );

            remainingCollection -= appliedAmount;
        }

        return remainingCollection;
    }

    private double customerPendingBalance(
            Long customerId
    ) {

        return saleRepository.findByCustomerIdAndPaymentStatusInOrderBySaleDateAsc(
                        customerId,
                        List.of(
                                PaymentStatus.PENDING,
                                PaymentStatus.PARTIAL
                        )
                )
                .stream()
                .mapToDouble(this::salePending)
                .sum();
    }

    private double exchangeCredit(
            ExchangeType exchangeType,
            double exchangeBoxes,
            double unitPrice
    ) {

        if (exchangeBoxes <= 0 ||
                unitPrice <= 0) {

            return 0;
        }

        if (exchangeType == ExchangeType.ONE_ON_ONE) {

            return exchangeBoxes * unitPrice;
        }

        if (exchangeType == ExchangeType.TWO_ON_ONE) {

            return exchangeBoxes * unitPrice * 0.5;
        }

        return 0;
    }

    private double saleTotal(
            Sale sale
    ) {

        return sale.getTotalAmount() == null
                ? 0
                : sale.getTotalAmount();
    }

    private double saleCollected(
            Sale sale
    ) {

        return sale.getAmountCollected() == null
                ? 0
                : sale.getAmountCollected();
    }

    private double salePending(
            Sale sale
    ) {

        if (sale.getPendingAmount() != null) {

            return sale.getPendingAmount();
        }

        return saleTotal(sale) - saleCollected(sale);
    }

    private void applyUpdatedBy(
            Sale sale,
            Authentication authentication
    ) {

        sale.setUpdatedByUserId(
                currentUserId(
                        authentication
                )
        );
        sale.setUpdatedByName(
                currentUserName(
                        authentication
                )
        );
        sale.setUpdatedByEmail(
                currentUserEmail(
                        authentication
                )
        );
        sale.setUpdatedAt(
                BusinessTime.now()
        );
    }

    private CashCollector resolveCashCollector(
            SaleRequest request,
            Authentication authentication
    ) {

        User currentUser =
                currentUser(authentication);

        if (isSuperAdmin(currentUser) &&
                request.getCollectorUserId() != null) {

            User collector =
                    userRepository.findById(request.getCollectorUserId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "User",
                                            request.getCollectorUserId()
                                    )
                            );

            return new CashCollector(
                    collector.getId(),
                    collector.getName(),
                    collector.getEmail()
            );
        }

        if (isSuperAdmin(currentUser) &&
                request.getCollectorName() != null &&
                !request.getCollectorName().isBlank()) {

            return new CashCollector(
                    request.getCollectorUserId(),
                    request.getCollectorName().trim(),
                    optionalText(request.getCollectorEmail())
            );
        }

        return new CashCollector(
                currentUserId(authentication),
                currentUserName(authentication),
                currentUserEmail(authentication)
        );
    }

    private User currentUser(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {
            return user;
        }

        return null;
    }

    private boolean isSuperAdmin(
            User user
    ) {

        return user != null &&
                user.getRole() != null &&
                "SUPER_ADMIN".equals(user.getRole().name());
    }

    private String optionalText(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private Long saleCollectorUserId(
            Sale sale
    ) {

        return sale.getCollectorUserId() == null
                ? sale.getCreatedByUserId()
                : sale.getCollectorUserId();
    }

    private String saleCollectorName(
            Sale sale
    ) {

        return sale.getCollectorName() == null ||
                sale.getCollectorName().isBlank()
                ? sale.getCreatedByName()
                : sale.getCollectorName();
    }

    private String saleCollectorEmail(
            Sale sale
    ) {

        return sale.getCollectorEmail() == null ||
                sale.getCollectorEmail().isBlank()
                ? sale.getCreatedByEmail()
                : sale.getCollectorEmail();
    }

    private Long currentUserId(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {

            return user.getId();
        }

        return 0L;
    }

    private String currentUserName(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {

            return user.getName();
        }

        return "SYSTEM";
    }

    private String currentUserEmail(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {

            return user.getEmail();
        }

        return "SYSTEM";
    }

    private record CashCollector(
            Long userId,
            String name,
            String email
    ) {}

    private Product resolveProduct(
            SaleRequest request,
            double unitPrice
    ) {

        if (request.getProductId() != null) {

            return productRepository.findById(
                    request.getProductId()
            ).orElseThrow(
                    () -> new ResourceNotFoundException(
                            "Product",
                            request.getProductId()
                    )
            );
        }

        String productName =
                request.getProductName() == null
                        ? ""
                        : request.getProductName().trim();

        if (productName.isBlank()) {
            throw new IllegalArgumentException(
                    "Product is required"
            );
        }

        return productRepository.findFirstByProductNameIgnoreCase(
                        productName
                )
                .map(product -> {
                    if (product.getActive() == null ||
                            !product.getActive()) {
                        product.setActive(true);
                    }

                    if (product.getStandardPrice() == null &&
                            unitPrice > 0) {
                        product.setStandardPrice(unitPrice);
                    }

                    return productRepository.save(product);
                })
                .orElseGet(() -> productRepository.save(
                        Product.builder()
                                .productCode(generateProductCode())
                                .productName(productName)
                                .unitType(ProductUnitType.BOX)
                                .standardPrice(unitPrice > 0
                                        ? unitPrice
                                        : 50)
                                .active(true)
                                .createdAt(BusinessTime.now())
                                .build()
                ));
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
                .createdByUserId(
                        sale.getCreatedByUserId()
                )
                .createdByName(
                        sale.getCreatedByName()
                )
                .createdByEmail(
                        sale.getCreatedByEmail()
                )
                .collectorUserId(
                        saleCollectorUserId(sale)
                )
                .collectorName(
                        saleCollectorName(sale)
                )
                .collectorEmail(
                        saleCollectorEmail(sale)
                )
                .updatedByUserId(
                        sale.getUpdatedByUserId()
                )
                .updatedByName(
                        sale.getUpdatedByName()
                )
                .updatedByEmail(
                        sale.getUpdatedByEmail()
                )
                .updatedAt(
                        sale.getUpdatedAt()
                )
                .saleDate(
                        sale.getSaleDate()
                )
                .build();
    }
}
