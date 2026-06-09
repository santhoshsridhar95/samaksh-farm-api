package com.samaksh.farms.dashboard.service;

import com.samaksh.farms.dashboard.dto.DashboardResponse;
import com.samaksh.farms.enums.OrderStatus;
import com.samaksh.farms.enums.PaymentStatus;
import com.samaksh.farms.order.entity.CustomerOrder;
import com.samaksh.farms.order.repo.CustomerOrderRepository;
import com.samaksh.farms.products.repo.ProductRepository;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;

    private final ProductRepository productRepository;

    private final CustomerOrderRepository orderRepository;

    private final SaleRepository saleRepository;

    public DashboardResponse getDashboard() {

        long totalCustomers =
                customerRepository.count();

        long totalProducts =
                productRepository.count();

        long totalOrders =
                orderRepository.count();

        long pendingOrders =
                orderRepository.findAll()
                        .stream()
                        .filter(order ->
                                order.getStatus()
                                        == OrderStatus.PENDING
                        )
                        .count();

        long fulfilledOrders =
                orderRepository.findAll()
                        .stream()
                        .filter(order ->
                                order.getStatus()
                                        == OrderStatus.FULFILLED
                        )
                        .count();

        long totalSales =
                saleRepository.count();

        double totalRevenue =
                saleRepository.findAll()
                        .stream()
                        .mapToDouble(
                                Sale::getTotalAmount
                        )
                        .sum();

        double pendingRevenue =
                saleRepository.findAll()
                        .stream()
                        .filter(sale ->
                                sale.getPaymentStatus()
                                        == PaymentStatus.PENDING
                        )
                        .mapToDouble(
                                Sale::getTotalAmount
                        )
                        .sum();

        return DashboardResponse.builder()
                .totalCustomers(
                        totalCustomers
                )
                .totalProducts(
                        totalProducts
                )
                .totalOrders(
                        totalOrders
                )
                .pendingOrders(
                        pendingOrders
                )
                .fulfilledOrders(
                        fulfilledOrders
                )
                .totalSales(
                        totalSales
                )
                .totalRevenue(
                        totalRevenue
                )
                .pendingRevenue(
                        pendingRevenue
                )
                .build();
    }
}