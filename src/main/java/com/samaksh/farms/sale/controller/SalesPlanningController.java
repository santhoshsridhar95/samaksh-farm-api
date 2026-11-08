package com.samaksh.farms.sale.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.sale.repo.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

/** Bounded history, independent of the paginated sales register. */
@RestController
@RequestMapping("/api/sales/planning-history")
@RequiredArgsConstructor
public class SalesPlanningController {
    private final SaleRepository sales;

    public record Key(LocalDate date, Long customerId, String product) {}
    public record Daily(LocalDate date, Long customerId, String product,
                        double boxes, double exchanges, double returns) {}
    public record History(LocalDate today, LocalDate from, List<Daily> days) {}

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<History> history() {
        var today = BusinessTime.today();
        var from = today.minusDays(63);
        Map<Key, double[]> totals = new LinkedHashMap<>();
        for (var sale : sales.findBySaleDateGreaterThanEqualAndSaleDateLessThan(
                from.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
            if ("DIRECT".equals(sale.getSalesChannel()) || sale.getCustomer() == null
                    || sale.getProduct() == null) continue;
            double boxes = value(sale.getQuantity());
            double exchanges = value(sale.getExchangeBoxes());
            double returns = value(sale.getReturnedBoxes());
            if (boxes == 0 && exchanges == 0 && returns == 0) continue;
            var key = new Key(sale.getSaleDate().toLocalDate(), sale.getCustomer().getId(),
                    sale.getProduct().getProductName());
            var total = totals.computeIfAbsent(key, ignored -> new double[3]);
            total[0] += boxes; total[1] += exchanges; total[2] += returns;
        }
        var days = totals.entrySet().stream().map(e -> new Daily(e.getKey().date(),
                e.getKey().customerId(), e.getKey().product(), e.getValue()[0],
                e.getValue()[1], e.getValue()[2])).toList();
        return ApiResponse.<History>builder().success(true).message("Planning history fetched")
                .data(new History(today, from, days)).build();
    }

    private static double value(Double value) { return value == null ? 0 : Math.max(0, value); }
}
