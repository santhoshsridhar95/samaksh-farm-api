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
}
