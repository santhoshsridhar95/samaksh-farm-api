package com.samaksh.farms.sourcing;
import com.samaksh.farms.finance.*;
import com.samaksh.farms.sale.repo.SaleRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SourcingReportServiceTest {
 BigDecimal n(String s){return new BigDecimal(s);}
 final LocalDate date=LocalDate.of(2026,11,9);
 SourcingReportService.SourceLine source(long id,String name,String kind,String boxes,String cost){return new SourcingReportService.SourceLine(id,date,id,name,kind,"Mushrooms",n(boxes),n(boxes).divide(n("5")),n(cost),n(boxes).multiply(n(cost)),n("0"),n("0"));}
 @Test void poolsDifferentVendorAndOwnFarmCostsExactly(){var sources=List.of(source(1,"A","VENDOR","10","37"),source(2,"B","VENDOR","10","36"),source(3,"C","VENDOR","10","38"),source(4,"Farm","OWN_FARM","20","25"));var sale=new SourcingReportService.SaleLine(1L,date,"Shop","Mushrooms","BOX",n("50"),n("2500"));var day=SourcingReportService.day(date,sources,List.of(sale),List.of(new SourcingReportService.ExpenseLine(1L,date,"Salary",n("100"))));assertEquals(n("1610.00"),day.goodsCost());assertEquals(n("890.00"),day.grossProfit());assertEquals(n("790.00"),day.netProfit());assertEquals(n("30"),day.vendorBoxes());assertEquals(n("20"),day.farmBoxes());}
 @Test void monthlyTotalsSumDailyResults(){var r=source(1,"A","VENDOR","10","37");var day=SourcingReportService.day(date,List.of(r),List.of(),List.of());var total=SourcingReportService.total(List.of(day,day));assertEquals(n("740.00"),total.get("goodsCost"));assertEquals(n("-740.00"),total.get("netProfit"));}
 @Test void excludesVendorPaymentsAndAdvancesFromOtherExpensesAndComparesWholeMonths(){var receipts=mock(SourcingReceiptRepository.class);var vendors=mock(SourcingVendorRepository.class);var sales=mock(SaleRepository.class);var entries=mock(MoneyEntryRepository.class);var payments=mock(SourcingPaymentRepository.class);var recoveries=mock(SourcingRecoveryRepository.class);var vendorPayment=new MoneyEntry();vendorPayment.setId(1L);vendorPayment.setType("EXPENSE");vendorPayment.setRequestKey("source:pay:1");vendorPayment.setDate(date);vendorPayment.setAmount(n("100"));var advance=new MoneyEntry();advance.setId(2L);advance.setType("VENDOR_ADVANCE");advance.setDate(date);advance.setAmount(n("300"));var expense=new MoneyEntry();expense.setId(3L);expense.setType("EXPENSE");expense.setDate(date);expense.setAmount(n("50"));when(entries.findAllByOrderByDateDescIdDesc()).thenReturn(List.of(vendorPayment,advance,expense));var service=new SourcingReportService(receipts,vendors,sales,entries,payments,recoveries);var report=service.report(LocalDate.of(2026,11,1),LocalDate.of(2026,11,30));assertEquals(LocalDate.of(2026,10,31),report.get("previousTo"));assertEquals(n("50.00"),((Map<?,?>)report.get("totals")).get("overhead"));assertEquals(1,((List<?>)report.get("expenses")).size());}
}
