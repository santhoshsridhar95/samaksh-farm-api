package com.samaksh.farms.analytics.service;

import com.samaksh.farms.analytics.dto.FarmDashboardResponse;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.TransactionType;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmDashboardService {

    private final InventoryTransactionRepository inventoryRepository;

    private final ProductionBatchRepository batchRepository;

    private final SaleRepository saleRepository;

    private final CustomerRepository customerRepository;

    public FarmDashboardResponse getDashboard() {

        double spawnBalance =
                calculateInventoryBalance(
                        InventoryType.SPAWN
                );

        double pelletBalance =
                calculateInventoryBalance(
                        InventoryType.PELLET
                );

        double bagBalance =
                calculateInventoryBalance(
                        InventoryType.BAG
                );

        int darkRoom =
                batchRepository.findAll()
                        .stream()
                        .mapToInt(
                                batch ->
                                        batch.getDarkRoomBags() == null
                                                ? 0
                                                : batch.getDarkRoomBags()
                        )
                        .sum();

        int lightRoom =
                batchRepository.findAll()
                        .stream()
                        .mapToInt(
                                batch ->
                                        batch.getLightRoomBags() == null
                                                ? 0
                                                : batch.getLightRoomBags()
                        )
                        .sum();

        int contaminated =
                batchRepository.findAll()
                        .stream()
                        .mapToInt(
                                batch ->
                                        batch.getContaminatedBags() == null
                                                ? 0
                                                : batch.getContaminatedBags()
                        )
                        .sum();

        int discarded =
                batchRepository.findAll()
                        .stream()
                        .mapToInt(
                                batch ->
                                        batch.getDiscardedBags() == null
                                                ? 0
                                                : batch.getDiscardedBags()
                        )
                        .sum();

        List<Sale> sales =
                saleRepository.findAll();

        double totalRevenue =
                sales.stream()
                        .mapToDouble(this::saleTotal)
                        .sum();

        double totalPendingAmount =
                sales.stream()
                        .mapToDouble(this::salePending)
                        .sum();

        LocalDate today =
                BusinessTime.today();

        return FarmDashboardResponse.builder()
                .spawnBalance(spawnBalance)
                .pelletBalance(pelletBalance)
                .bagBalance(bagBalance)
                .darkRoomBags(darkRoom)
                .lightRoomBags(lightRoom)
                .contaminatedBags(contaminated)
                .discardedBags(discarded)
                .totalCustomers(
                        customerRepository.count()
                )
                .totalRevenue(totalRevenue)
                .totalPendingAmount(totalPendingAmount)
                .dailyTopShop(
                        topShopForPeriod(
                                sales,
                                today,
                                today
                        )
                )
                .weeklyTopShop(
                        topShopForPeriod(
                                sales,
                                today.minusDays(6),
                                today
                        )
                )
                .monthlyTopShop(
                        topShopForPeriod(
                                sales,
                                today.withDayOfMonth(1),
                                today
                        )
                )
                .shopBalances(
                        shopBalances(sales)
                )
                .build();
    }

    private List<FarmDashboardResponse.ShopBalanceResponse> shopBalances(
            List<Sale> sales
    ) {

        Map<Long, List<Sale>> salesByCustomer =
                sales.stream()
                        .filter(sale -> sale.getCustomer() != null)
                        .collect(
                                Collectors.groupingBy(
                                        sale -> sale.getCustomer()
                                                .getId()
                                )
                        );

        return salesByCustomer.values()
                .stream()
                .map(customerSales -> {

                    Sale firstSale =
                            customerSales.get(0);

                    return FarmDashboardResponse.ShopBalanceResponse
                            .builder()
                            .customerId(
                                    firstSale.getCustomer()
                                            .getId()
                            )
                            .shopName(
                                    firstSale.getCustomer()
                                            .getCustomerName()
                            )
                            .shopCategory(
                                    firstSale.getCustomer()
                                            .getShopCategory()
                            )
                            .location(
                                    firstSale.getCustomer()
                                            .getLocation() == null
                                            ? "R.T. Nagar"
                                            : firstSale.getCustomer()
                                            .getLocation()
                            )
                            .totalAmount(
                                    customerSales.stream()
                                            .mapToDouble(this::saleTotal)
                                            .sum()
                            )
                            .collectedAmount(
                                    customerSales.stream()
                                            .mapToDouble(this::saleCollected)
                                            .sum()
                            )
                            .pendingAmount(
                                    customerSales.stream()
                                            .mapToDouble(this::salePending)
                                            .sum()
                            )
                            .totalBoxes(
                                    customerSales.stream()
                                            .mapToDouble(this::saleQuantity)
                                            .sum()
                            )
                            .totalKgs(
                                    customerSales.stream()
                                            .mapToDouble(this::saleQuantity)
                                            .sum() * 0.2
                            )
                            .build();
                })
                .sorted(
                        Comparator.comparing(
                                FarmDashboardResponse.ShopBalanceResponse::getPendingAmount
                        ).reversed()
                )
                .toList();
    }

    private String topShopForPeriod(
            List<Sale> sales,
            LocalDate startDate,
            LocalDate endDate
    ) {

        return sales.stream()
                .filter(sale -> sale.getCustomer() != null)
                .filter(sale -> sale.getSaleDate() != null)
                .filter(sale -> {

                    LocalDate saleDate =
                            sale.getSaleDate()
                                    .toLocalDate();

                    return !saleDate.isBefore(startDate) &&
                            !saleDate.isAfter(endDate);
                })
                .collect(
                        Collectors.groupingBy(
                                sale -> sale.getCustomer()
                                        .getCustomerName(),
                                Collectors.summingDouble(this::saleTotal)
                        )
                )
                .entrySet()
                .stream()
                .max(
                        Map.Entry.comparingByValue()
                )
                .map(entry -> entry.getKey()
                        + " - Rs. "
                        + entry.getValue())
                .orElse("No sales yet");
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

    private double saleQuantity(
            Sale sale
    ) {

        return sale.getQuantity() == null
                ? 0
                : sale.getQuantity();
    }

    private double calculateInventoryBalance(
            InventoryType inventoryType
    ) {

        return inventoryRepository
                .findByInventoryType(
                        inventoryType
                )
                .stream()
                .mapToDouble(transaction -> {

                    if (transaction.getTransactionType()
                            == TransactionType.PURCHASE) {

                        return transaction.getQuantity();
                    }

                    return -transaction.getQuantity();
                })
                .sum();
    }
}
