package com.samaksh.farms.sourcing;
import com.samaksh.farms.finance.*;
import com.samaksh.farms.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class SourcingPaymentTest {
 BigDecimal n(String s){return new BigDecimal(s);}
 final SourcingVendorRepository vendors=mock(SourcingVendorRepository.class);
 final SourcingReceiptRepository receipts=mock(SourcingReceiptRepository.class);
 final SourcingPaymentRepository payments=mock(SourcingPaymentRepository.class);
 final SourcingRecoveryRepository recoveries=mock(SourcingRecoveryRepository.class);
 final MoneyService money=mock(MoneyService.class);
 final SourcingService service=new SourcingService(vendors,receipts,payments,recoveries,mock(SourcingSettingsRepository.class),money,mock(MoneyAccountRepository.class),mock(AuditService.class));
 SourcingPaymentTest(){var v=new SourcingVendor();v.setId(1L);v.setKind("VENDOR");v.setName("Grower");when(vendors.findById(1L)).thenReturn(Optional.of(v));var r=new SourcingReceipt();r.setId(2L);r.setVendorId(1L);r.setDate(LocalDate.now());r.setQuantityKg(n("10"));r.setBoxesPerKg(n("5"));r.setCostPerKg(n("180"));r.setSellingPricePerKg(n("250"));r.setTransportCost(n("100"));r.setOtherCost(n("0"));r.setExpensesBorneBy("VENDOR");r.setExpensesPaidBy("FARM");r.setSoldKg(n("0"));when(receipts.findById(2L)).thenReturn(Optional.of(r));when(payments.findAll()).thenReturn(List.of());var entry=new MoneyEntry();entry.setId(99L);when(money.record(any(),any())).thenReturn(entry);when(money.vendorFunds(any(),any(),any(),any(),anyBoolean(),any(),any())).thenReturn(entry);when(payments.save(any())).thenAnswer(a->{SourcingPayment p=a.getArgument(0);p.setId(3L);return p;});}
 SourcingRequests.Payment request(String amount){return new SourcingRequests.Payment("key",2L,7L,n(amount),LocalDate.now(),"Payment");}
 @Test void fullPaymentUsesChosenAccountAndLinksLedger(){var p=service.payment(null,request("1700"),null);assertEquals(n("1700"),p.getAmount());assertEquals(7L,p.getSourceAccountId());assertEquals(99L,p.getMoneyEntryId());verify(money).record(argThat(r->r.sourceId()==7L && r.amount().compareTo(n("1700"))==0 && r.type().equals("EXPENSE")),isNull());verify(payments).save(p);}
 @Test void paymentCannotExceedDueAfterDeduction(){assertThrows(IllegalArgumentException.class,()->service.payment(null,request("1800"),null));verify(money,never()).record(any(),any());}
 @Test void duplicatePaymentDoesNotDebitAgain(){var prior=new SourcingPayment();prior.setRequestFingerprint(MoneyService.fingerprint(request("500")));when(payments.findByRequestKey("key")).thenReturn(Optional.of(prior));assertSame(prior,service.payment(null,request("500"),null));verify(money,never()).record(any(),any());}
 @Test void additionalPaymentIsSeparateFromInvoiceExpense(){var p=service.payment(null,new SourcingRequests.Payment("extra",2L,7L,n("1700"),LocalDate.now(),"Extra",n("300")),null);assertEquals(n("300"),p.getAdditionalAmount());verify(money).record(argThat(r->r.amount().compareTo(n("1700"))==0),isNull());verify(money).vendorFunds(anyString(),eq(7L),eq(n("300")),any(),eq(false),anyString(),isNull());}
 @Test void partialRecoveryCreditsSelectedAccount(){var p=new SourcingPayment();p.setId(3L);p.setAdditionalAmount(n("300"));p.setDate(LocalDate.now());when(payments.findById(3L)).thenReturn(Optional.of(p));var result=service.recovery(null,new SourcingRequests.Recovery("refund",3L,9L,n("100"),LocalDate.now(),"Partial refund"),null);assertEquals(n("100"),result.getAmount());verify(money).vendorFunds(anyString(),eq(9L),eq(n("100")),any(),eq(true),anyString(),isNull());verify(recoveries).save(result);}
 @Test void recoveryCannotExceedRemainingAdvance(){var p=new SourcingPayment();p.setId(3L);p.setAdditionalAmount(n("300"));p.setDate(LocalDate.now());when(payments.findById(3L)).thenReturn(Optional.of(p));var earlier=new SourcingRecovery();earlier.setId(4L);earlier.setAmount(n("200"));when(recoveries.findByPaymentId(3L)).thenReturn(List.of(earlier));assertThrows(IllegalArgumentException.class,()->service.recovery(null,new SourcingRequests.Recovery("refund",3L,9L,n("101"),LocalDate.now(),""),null));verify(money,never()).vendorFunds(any(),any(),any(),any(),anyBoolean(),any(),any());}
 @Test void editedAdvanceCannotBeLessThanRecovered(){var p=new SourcingPayment();p.setId(3L);p.setReceiptId(2L);when(payments.findById(3L)).thenReturn(Optional.of(p));var recovered=new SourcingRecovery();recovered.setAmount(n("200"));recovered.setDate(LocalDate.now());when(recoveries.findByPaymentId(3L)).thenReturn(List.of(recovered));assertThrows(IllegalArgumentException.class,()->service.payment(3L,new SourcingRequests.Payment("edit",2L,7L,n("1700"),LocalDate.now(),"",n("100")),null));verify(money,never()).reverseLinked(any(),any(),any());}

 @Test void plannedSellingDateCanBeFutureAndDoesNotChangeReceivedDate(){
  var today=com.samaksh.farms.common.time.BusinessTime.today();
  var r=new SourcingRequests.Receipt("plan",1L,today,"Mushrooms","KG",n("10"),n("180"),n("100"),n("0"),"VENDOR","FARM",null,n("0"),"",n("0"),today.plusDays(1));
  var result=service.receipt(2L,r,null).receipt();assertEquals(today,result.getDate());assertEquals(today.plusDays(1),result.getPlannedSellingDate());verify(receipts).save(result);
 }
 @Test void plannedSellingDateCannotPrecedeArrival(){
  var today=com.samaksh.farms.common.time.BusinessTime.today();
  var r=new SourcingRequests.Receipt("plan",1L,today,"Mushrooms","KG",n("10"),n("180"),n("100"),n("0"),"VENDOR","FARM",null,n("0"),"",n("0"),today.minusDays(1));
  assertThrows(IllegalArgumentException.class,()->service.receipt(2L,r,null));verify(receipts,never()).save(any());
 }

 @Test void paymentBeforeReceiptPreservesActualDate() {
  var date=LocalDate.now().minusMonths(1);
  var result=service.payment(null,new SourcingRequests.Payment("early",2L,7L,n("500"),date,"Paid before delivery"),null);
  assertEquals(date,result.getDate());
  verify(money).record(argThat(r->r.date().equals(date)),isNull());
 }
 @Test void splitVendorPaymentSeparatesInvoiceAndAdvanceAndRetriesSafely() {
  var saved=new ArrayList<SourcingPayment>();
  when(payments.findAll()).thenAnswer(i->new ArrayList<>(saved));
  when(payments.findByRequestKey(anyString())).thenAnswer(i->saved.stream().filter(p->p.getRequestKey().equals(i.getArgument(0))).findFirst());
  doAnswer(i->{SourcingPayment p=i.getArgument(0);p.setId((long)saved.size()+1);saved.add(p);return p;}).when(payments).save(any());
  var r=new SourcingRequests.SplitPayment(new SourcingRequests.Payment("split-vendor",2L,7L,n("1700"),LocalDate.now().minusDays(2),"Split",n("300")),List.of(new FundingAllocation(7L,n("800")),new FundingAllocation(8L,n("1200"))));
  var result=service.splitPayment(r,null);
  assertEquals(2,result.size());assertEquals(n("800"),result.get(0).getAmount());assertEquals(n("900"),result.get(1).getAmount());
  assertEquals(n("300"),result.get(1).getAdditionalAmount());
  verify(money).validateFunding(r.sources());
  verify(money).record(argThat(p->p.sourceId()==8L && p.amount().equals(n("900"))),isNull());
  verify(money).vendorFunds(anyString(),eq(8L),eq(n("300")),any(),eq(false),anyString(),isNull());
  service.splitPayment(r,null);assertEquals(2,saved.size());
  var changed=new SourcingRequests.SplitPayment(r.payment(),List.of(new FundingAllocation(7L,n("900")),new FundingAllocation(8L,n("1100"))));
  assertThrows(IllegalArgumentException.class,()->service.splitPayment(changed,null));
 }
 @Test void splitVendorFailureValidatesTotalDueAndFundsBeforeSaving() {
  var r=new SourcingRequests.SplitPayment(request("1800"),List.of(new FundingAllocation(7L,n("1800"))));
  assertThrows(IllegalArgumentException.class,()->service.splitPayment(r,null));
  doThrow(new IllegalArgumentException("Insufficient funds")).when(money).validateFunding(any());
  var valid=new SourcingRequests.SplitPayment(request("500"),List.of(new FundingAllocation(7L,n("500"))));
  assertThrows(IllegalArgumentException.class,()->service.splitPayment(valid,null));
  verify(payments,never()).save(any());verify(money,never()).record(any(),any());
 }
}
