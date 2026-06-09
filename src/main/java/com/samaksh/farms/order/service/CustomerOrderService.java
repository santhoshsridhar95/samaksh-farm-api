package com.samaksh.farms.order.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.enums.OrderStatus;
import com.samaksh.farms.enums.PaymentStatus;
import com.samaksh.farms.order.dto.CustomerOrderRequest;
import com.samaksh.farms.order.dto.CustomerOrderResponse;
import com.samaksh.farms.order.dto.PagedResponse;
import com.samaksh.farms.order.entity.CustomerOrder;
import com.samaksh.farms.order.repo.CustomerOrderRepository;
import com.samaksh.farms.products.entity.Product;
import com.samaksh.farms.products.repo.ProductRepository;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;

    private final CustomerRepository customerRepository;

    private final ProductRepository productRepository;

    private final SaleRepository saleRepository;

    private final AuditService auditService;

    public CustomerOrderResponse createOrder(
            CustomerOrderRequest request,
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

        double expectedAmount =
                request.getQuantity()
                        * request.getExpectedUnitPrice();

        CustomerOrder order =
                CustomerOrder.builder()
                        .orderCode(
                                generateOrderCode()
                        )
                        .customer(customer)
                        .product(product)
                        .quantity(
                                request.getQuantity()
                        )
                        .expectedUnitPrice(
                                request.getExpectedUnitPrice()
                        )
                        .expectedAmount(
                                expectedAmount
                        )
                        .expectedDeliveryDate(
                                request.getExpectedDeliveryDate()
                        )
                        .status(
                                OrderStatus.PENDING
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        CustomerOrder savedOrder =
                orderRepository.save(order);

        auditService.createAudit(
                authentication,
                "ORDER",
                "CREATE_ORDER",
                savedOrder.getOrderCode(),
                "Order Created"
        );

        return mapToResponse(savedOrder);
    }

    public CustomerOrderResponse fulfillOrder(
            Long orderId,
            Authentication authentication
    ) {

        CustomerOrder order =
                orderRepository.findById(orderId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Order",
                                                orderId
                                        )
                        );

        if (order.getStatus()
                == OrderStatus.FULFILLED) {

            throw new RuntimeException(
                    "Order already fulfilled"
            );
        }

        Sale sale =
                Sale.builder()
                        .customer(
                                order.getCustomer()
                        )
                        .product(
                                order.getProduct()
                        )
                        .quantity(
                                order.getQuantity()
                        )
                        .unitPrice(
                                order.getExpectedUnitPrice()
                        )
                        .totalAmount(
                                order.getExpectedAmount()
                        )
                        .paymentStatus(
                                PaymentStatus.PENDING
                        )
                        .remarks(
                                "Generated from Order "
                                        + order.getOrderCode()
                        )
                        .saleDate(
                                LocalDateTime.now()
                        )
                        .build();

        saleRepository.save(
                sale
        );

        order.setStatus(
                OrderStatus.FULFILLED
        );

        CustomerOrder savedOrder =
                orderRepository.save(
                        order
                );

        auditService.createAudit(
                authentication,
                "ORDER",
                "FULFILL_ORDER",
                savedOrder.getOrderCode(),
                "Order Fulfilled"
        );

        return mapToResponse(
                savedOrder
        );
    }

    private CustomerOrderResponse mapToResponse(
            CustomerOrder order
    ) {

        return CustomerOrderResponse.builder()
                .id(
                        order.getId()
                )
                .orderCode(
                        order.getOrderCode()
                )
                .customerName(
                        order.getCustomer()
                                .getCustomerName()
                )
                .productName(
                        order.getProduct()
                                .getProductName()
                )
                .quantity(
                        order.getQuantity()
                )
                .expectedUnitPrice(
                        order.getExpectedUnitPrice()
                )
                .expectedAmount(
                        order.getExpectedAmount()
                )
                .expectedDeliveryDate(
                        order.getExpectedDeliveryDate()
                )
                .status(
                        order.getStatus()
                )
                .remarks(
                        order.getRemarks()
                )
                .build();
    }

    private String generateOrderCode() {

        long count =
                orderRepository.count() + 1;

        return "SF-ORD-"
                + String.format(
                "%04d",
                count
        );
    }

    public PagedResponse<CustomerOrderResponse> getOrders(
            int page,
            int size,
            OrderStatus status
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        Page<CustomerOrder> orders;

        if (status != null) {

            orders =
                    orderRepository.findByStatus(
                            status,
                            pageable
                    );

        } else {

            orders =
                    orderRepository.findAll(
                            pageable
                    );
        }

        return PagedResponse
                .<CustomerOrderResponse>builder()
                .content(
                        orders.getContent()
                                .stream()
                                .map(this::mapToResponse)
                                .toList()
                )
                .page(
                        orders.getNumber()
                )
                .size(
                        orders.getSize()
                )
                .totalElements(
                        orders.getTotalElements()
                )
                .totalPages(
                        orders.getTotalPages()
                )
                .last(
                        orders.isLast()
                )
                .build();
    }
}