package com.samaksh.farms.analytics.service;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.enums.ExchangeType;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ShopPriceServiceTest {
 Sale sale(ExchangeType type,double exchanges){return Sale.builder().id(1L).quantity(10d).unitPrice(50d).exchangeBoxes(exchanges).exchangeType(type).totalAmount(400d).build();}
 @Test void fullExchangeCreditsAreCountedOnce(){var line=ShopPriceService.line(sale(ExchangeType.ONE_ON_ONE,2));assertEquals(new BigDecimal("100.00"),line.exchangeCredit());assertEquals(new BigDecimal("500.00"),line.grossValue());assertEquals(new BigDecimal("400.00"),line.revenue());assertEquals(0,line.revenue().divide(line.quantity()).compareTo(new BigDecimal("40")));}
 @Test void twoForOneCreditsHalfAndCapsAtInvoiceGross(){assertEquals(new BigDecimal("50.00"),ShopPriceService.line(sale(ExchangeType.TWO_ON_ONE,2)).exchangeCredit());assertEquals(new BigDecimal("500.00"),ShopPriceService.line(sale(ExchangeType.ONE_ON_ONE,100)).exchangeCredit());assertEquals(new BigDecimal("0.00"),ShopPriceService.line(sale(ExchangeType.NONE,2)).exchangeCredit());}
 @Test void historyFiltersExcludeCollectionsAndUseSaleDate(){var repo=mock(SaleRepository.class);var current=sale(ExchangeType.NONE,0);current.setSaleDate(LocalDate.of(2026,11,9).atStartOfDay());var old=sale(ExchangeType.NONE,0);old.setSaleDate(LocalDate.of(2026,7,1).atStartOfDay());var collection=Sale.builder().salesChannel("COLLECTION").quantity(0d).build();when(repo.findAll()).thenReturn(List.of(current,old,collection));when(repo.findBySaleDateGreaterThanEqualAndSaleDateLessThan(any(),any())).thenReturn(List.of(current,collection));var service=new ShopPriceService(repo);assertEquals(1,service.report(LocalDate.of(2026,11,1),LocalDate.of(2026,11,30)).size());assertEquals(2,service.report(null,null).size());}
}
