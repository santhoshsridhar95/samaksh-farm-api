package com.samaksh.farms.sale.controller;

import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.products.entity.Product;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalesPlanningControllerTest {
    @Test void boundedHistoryAggregatesDeliveriesAndExcludesDirectSalesAndCollections() {
        var repo = mock(SaleRepository.class);
        var customer = new Customer(); customer.setId(1L);
        var product = new Product(); product.setProductName("Oyster");
        var today = BusinessTime.today();
        var first = Sale.builder().customer(customer).product(product).saleDate(today.atStartOfDay())
                .quantity(10.0).exchangeBoxes(2.0).returnedBoxes(1.0).build();
        var second = Sale.builder().customer(customer).product(product).saleDate(today.atTime(12, 0))
                .quantity(20.0).build();
        var direct = Sale.builder().customer(customer).product(product).saleDate(today.atStartOfDay())
                .quantity(100.0).salesChannel("DIRECT").build();
        var collection = Sale.builder().customer(customer).product(product).saleDate(today.atStartOfDay())
                .quantity(0.0).build();
        when(repo.findBySaleDateGreaterThanEqualAndSaleDateLessThan(today.minusDays(63).atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(first, second, direct, collection));
        var history = new SalesPlanningController(repo).history().getData();
        assertEquals(1, history.days().size());
        assertEquals(30, history.days().getFirst().boxes());
        assertEquals(2, history.days().getFirst().exchanges());
        assertEquals(1, history.days().getFirst().returns());
        assertEquals(today.minusDays(63), history.from());
        verify(repo).findBySaleDateGreaterThanEqualAndSaleDateLessThan(today.minusDays(63).atStartOfDay(), today.plusDays(1).atStartOfDay());
    }
}
