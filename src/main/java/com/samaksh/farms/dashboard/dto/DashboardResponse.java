package com.samaksh.farms.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponse {

    private Long totalCustomers;

    private Long totalProducts;

    private Long totalOrders;

    private Long pendingOrders;

    private Long fulfilledOrders;

    private Long totalSales;

    private Double totalRevenue;

    private Double pendingRevenue;
}