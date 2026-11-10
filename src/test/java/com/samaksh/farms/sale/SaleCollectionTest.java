package com.samaksh.farms.sale;
import com.samaksh.farms.sale.service.SaleService;
import com.samaksh.farms.sale.dto.SaleRequest;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.products.repo.ProductRepository;
import com.samaksh.farms.user.repo.UserRepository;
import com.samaksh.farms.finance.MoneyService;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SaleCollectionTest {
 final SaleRepository repo=mock(SaleRepository.class);final MoneyService money=mock(MoneyService.class);final CustomerRepository customers=mock(CustomerRepository.class);final ProductRepository products=mock(ProductRepository.class);
 final SaleService service=new SaleService(money,repo,customers,products,mock(AuditService.class),mock(UserRepository.class));
 Sale first,second;
 SaleRequest request(double amount){var c=new Customer();c.setId(1L);c.setCustomerName("Shop A");when(customers.findById(1L)).thenReturn(Optional.of(c));first=Sale.builder().id(10L).customer(c).totalAmount(250d).amountCollected(0d).pendingAmount(250d).paymentStatus(PaymentStatus.PENDING).build();second=Sale.builder().id(11L).customer(c).totalAmount(500d).amountCollected(0d).pendingAmount(500d).paymentStatus(PaymentStatus.PENDING).build();when(repo.findByCustomerIdAndPaymentStatusInOrderBySaleDateAsc(eq(1L),any())).thenReturn(List.of(first,second));when(repo.save(any())).thenAnswer(a->{Sale s=a.getArgument(0);if(s.getId()==null)s.setId(12L);return s;});var r=new SaleRequest();r.setCustomerId(1L);r.setQuantity(0d);r.setAmountCollected(amount);return r;}
 @Test void zeroBoxesCollectsOldestBalanceWithoutNewRevenue(){var result=service.createSale(request(200),null);assertEquals(50,first.getPendingAmount());assertEquals(500,second.getPendingAmount());assertEquals(0,result.getTotalAmount());assertEquals(0,result.getQuantity());assertEquals(200,result.getAmountReceivedToday());assertEquals("COLLECTION",result.getSalesChannel());assertEquals(PaymentStatus.PAID,result.getPaymentStatus());verify(products,never()).save(any());verify(money).collection(any(),any(),any(),eq(200d),eq(12L),isNull());}
 @Test void collectionSpansOlderInvoices(){service.createSale(request(500),null);assertEquals(0,first.getPendingAmount());assertEquals(250,second.getPendingAmount());}
 @Test void cannotCollectMoreThanOutstanding(){assertThrows(IllegalArgumentException.class,()->service.createSale(request(800),null));verify(repo,never()).save(any());}
 @Test void emptyDeliveryIsRejected(){assertThrows(IllegalArgumentException.class,()->service.createSale(request(0),null));verify(repo,never()).save(any());}

 @Test void editsZeroBoxCollectionDateAndRemarksWithoutReapplyingMoney(){
  var request=request(200);service.createSale(request,null);
  var collection=Sale.builder().id(12L).customer(first.getCustomer()).salesChannel("COLLECTION").quantity(0d).unitPrice(50d).totalAmount(0d).amountCollected(0d).amountReceivedToday(200d).pendingAmount(0d).paymentStatus(PaymentStatus.PAID).build();
  when(repo.findById(12L)).thenReturn(Optional.of(collection));
  var user=new com.samaksh.farms.user.entity.User();user.setRole(com.samaksh.farms.enums.Role.SUPER_ADMIN);user.setEmail("admin@test");
  var auth=mock(org.springframework.security.core.Authentication.class);when(auth.getPrincipal()).thenReturn(user);
  request.setAmountCollected(0d);request.setAmountReceivedToday(200d);request.setProductName("Balance collection");request.setDeliveryDate(java.time.LocalDate.of(2026,11,9));request.setRemarks("Corrected remark");
  clearInvocations(money,products);var result=service.updateSaleEntry(12L,request,auth);
  assertEquals("Corrected remark",result.getRemarks());assertEquals("COLLECTION",result.getSalesChannel());assertEquals(0d,result.getQuantity());assertEquals(200d,result.getAmountReceivedToday());assertEquals(PaymentStatus.PAID,result.getPaymentStatus());assertEquals(50d,first.getPendingAmount());
  verify(products,never()).save(any());verify(money,never()).collection(any(),any(),any(),anyDouble(),any(),any());

 }

 @Test void correctionIncreasesDecreasesAndRetriesWithoutCountingCashTwice(){
  var r=request(200);service.createSale(r,null);
  var collection=Sale.builder().id(12L).customer(first.getCustomer()).salesChannel("COLLECTION").quantity(0d).unitPrice(0d).totalAmount(0d).amountCollected(0d).amountReceivedToday(200d).pendingAmount(0d).paymentStatus(PaymentStatus.PAID).build();
  when(repo.findById(12L)).thenReturn(Optional.of(collection));when(repo.findByCustomerIdOrderBySaleDateAscIdAsc(1L)).thenReturn(List.of(first,second,collection));
  var user=new com.samaksh.farms.user.entity.User();user.setRole(com.samaksh.farms.enums.Role.SUPER_ADMIN);var auth=mock(org.springframework.security.core.Authentication.class);when(auth.getPrincipal()).thenReturn(user);
  r.setProductName("Balance collection");r.setAmountReceivedToday(300d);service.updateSaleEntry(12L,r,auth);
  assertEquals(250d,first.getAmountCollected());assertEquals(50d,second.getAmountCollected());assertEquals(300d,collection.getAmountReceivedToday());
  service.updateSaleEntry(12L,r,auth);assertEquals(50d,second.getAmountCollected());
  r.setAmountReceivedToday(100d);service.updateSaleEntry(12L,r,auth);assertEquals(100d,first.getAmountCollected());assertEquals(0d,second.getAmountCollected());
  verify(money).correctCollection(eq(12L),any(),any(),any(),eq(100d),any(),eq(auth));verify(money).correctCollection(eq(12L),any(),any(),any(),eq(-200d),any(),eq(auth));
 }

 @Test void exactReportedPutPayloadAcceptsZeroBoxesAndThreeHundredReceipt()throws Exception{
  var customer=new Customer();customer.setId(10L);customer.setCustomerName("Ajji Angdi");
  var product=new com.samaksh.farms.products.entity.Product();product.setId(2L);product.setProductName("Fresh Oyster Mushroom");
  var invoice=Sale.builder().id(31L).customer(customer).product(product).quantity(10d).unitPrice(50d).totalAmount(500d).amountCollected(200d).pendingAmount(300d).salesChannel("SHOP").build();
  var collection=Sale.builder().id(32L).customer(customer).product(product).quantity(0d).unitPrice(50d).totalAmount(0d).amountCollected(0d).amountReceivedToday(200d).pendingAmount(0d).salesChannel("COLLECTION").build();
  when(customers.findById(10L)).thenReturn(Optional.of(customer));when(products.findById(2L)).thenReturn(Optional.of(product));when(repo.findById(32L)).thenReturn(Optional.of(collection));when(repo.findByCustomerIdOrderBySaleDateAscIdAsc(10L)).thenReturn(List.of(invoice,collection));when(repo.save(any())).thenAnswer(a->a.getArgument(0));
  var user=new com.samaksh.farms.user.entity.User();user.setRole(com.samaksh.farms.enums.Role.SUPER_ADMIN);user.setEmail("admin@test");var auth=mock(org.springframework.security.core.Authentication.class);when(auth.getPrincipal()).thenReturn(user);
  var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new com.samaksh.farms.sale.controller.SaleController(service)).setControllerAdvice(new com.samaksh.farms.common.exception.GlobalExceptionHandler()).build();
  String json="""
   {"customerId":10,"productId":2,"productName":"Fresh Oyster Mushroom","quantity":0,"unitPrice":50,"amountCollected":0,"amountReceivedToday":300,"deliveryDate":"2026-11-09","exchangeBoxes":0,"exchangeType":"NONE","remarks":"","returnedBoxes":0,"shopkeeperSellingPrice":60}
   """;
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/sales/32/entry").principal(auth).contentType("application/json").content(json)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.amountReceivedToday").value(300));
  assertEquals(300d,invoice.getAmountCollected());assertEquals(200d,invoice.getPendingAmount());assertEquals(0d,collection.getTotalAmount());
 }
}
