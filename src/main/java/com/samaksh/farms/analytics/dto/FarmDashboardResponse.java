package com.samaksh.farms.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FarmDashboardResponse {

    private Double spawnBalance;

    private Double pelletBalance;

    private Double bagBalance;

    private Integer darkRoomBags;

    private Integer lightRoomBags;

    private Integer contaminatedBags;

    private Integer discardedBags;

    private Long totalCustomers;

    private Double totalRevenue;

    private Double totalPendingAmount;

    private String dailyTopShop;

    private String weeklyTopShop;

    private String monthlyTopShop;

    private List<ShopBalanceResponse> shopBalances;

    @Data
    @Builder
    public static class ShopBalanceResponse {

        private Long customerId;

        private String shopName;

        private String shopCategory;

        private String location;

        private Double totalAmount;

        private Double collectedAmount;

        private Double pendingAmount;

        private Double totalBoxes;

        private Double totalKgs;
    }
}
