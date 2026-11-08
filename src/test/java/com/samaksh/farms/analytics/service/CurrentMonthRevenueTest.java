package com.samaksh.farms.analytics.service;
import com.samaksh.farms.sale.entity.Sale;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class CurrentMonthRevenueTest {
 Sale sale(String date,double amount,String channel){return Sale.builder().saleDate(LocalDate.parse(date).atStartOfDay()).totalAmount(amount).salesChannel(channel).build();}
 @Test void monthToDateIncludesDirectSalesAndExcludesOldCollectionsAndFutureDates(){assertEquals(800.30,FarmDashboardService.currentMonthRevenue(List.of(sale("2026-11-01",500.10,"SHOP"),sale("2026-11-09",300.20,"DIRECT"),sale("2026-10-31",900,"SHOP"),sale("2026-11-10",700,"SHOP"),sale("2026-11-09",100,"COLLECTION")),LocalDate.of(2026,11,9)),.00001);}
 @Test void noCurrentSalesIsZero(){assertEquals(0,FarmDashboardService.currentMonthRevenue(List.of(),LocalDate.of(2026,1,1)));}
}
