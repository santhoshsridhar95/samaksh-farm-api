package com.samaksh.farms.sale;
import com.samaksh.farms.sale.service.SaleService;
import com.samaksh.farms.sale.dto.SaleRequest;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.products.repo.ProductRepository;
import com.samaksh.farms.user.repo.UserRepository;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.finance.MoneyService;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.enums.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DirectSaleCorrectionTest {
 @Test void correctsAllPurchaseFieldsWithoutChangingOtherInvoicesOrCreator(){
  var repo=mock(SaleRepository.class);var money=mock(MoneyService.class);var customers=mock(CustomerRepository.class);var products=mock(ProductRepository.class);var users=mock(UserRepository.class);
  var service=new SaleService(money,repo,customers,products,mock(AuditService.class),users);
  var sale=Sale.builder().id(10L).salesChannel("DIRECT").createdByUserId(99L).amountCollected(50d).saleDate(LocalDateTime.of(2026,1,1,10,0)).build();when(repo.findById(10L)).thenReturn(Optional.of(sale));
  var c=Customer.builder().id(2L).customerType("DIRECT").build();when(customers.findById(2L)).thenReturn(Optional.of(c));
  var collector=User.builder().id(3L).name("Worker").email("worker@test").active(true).build();when(users.findById(3L)).thenReturn(Optional.of(collector));
  when(products.save(any())).thenAnswer(i->i.getArgument(0));
  var admin=User.builder().id(1L).role(Role.SUPER_ADMIN).build();var auth=mock(org.springframework.security.core.Authentication.class);when(auth.getPrincipal()).thenReturn(admin);
  var r=new SaleRequest();r.setCustomerId(2L);r.setCollectorUserId(3L);r.setProductName("New product");r.setQuantity(2d);r.setUnitPrice(60d);r.setAmountCollected(100d);r.setDeliveryDate(LocalDate.of(2026,1,2));r.setRemarks("Corrected");
  service.correctDirectSale(10L,r,"Market",auth);
  assertEquals(99L,sale.getCreatedByUserId());assertEquals(c,sale.getCustomer());assertEquals("New product",sale.getProduct().getProductName());assertEquals(2d,sale.getQuantity());assertEquals(60d,sale.getUnitPrice());assertEquals(120d,sale.getTotalAmount());assertEquals(20d,sale.getPendingAmount());assertEquals(PaymentStatus.PARTIAL,sale.getPaymentStatus());assertEquals(3L,sale.getCollectorUserId());assertEquals("Market",sale.getPurchaseLocation());assertEquals("Corrected",sale.getRemarks());assertEquals(r.getDeliveryDate(),sale.getSaleDate().toLocalDate());
  verify(repo,never()).findByCustomerIdOrderBySaleDateAscIdAsc(any());verify(money).correctDirectCollection(10L,3L,"Worker","worker@test",100d,r.getDeliveryDate(),auth);
  r.setAmountCollected(121d);assertThrows(IllegalArgumentException.class,()->service.correctDirectSale(10L,r,"Market",auth));
 }
}
