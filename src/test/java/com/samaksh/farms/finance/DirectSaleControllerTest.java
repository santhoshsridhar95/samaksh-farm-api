package com.samaksh.farms.finance;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.config.DatabaseConstraintRepair;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.dto.*;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.sale.service.SaleService;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectSaleControllerTest {
    CustomerRepository customers;
    SaleRepository sales;
    SaleService saleService;
    MoneyEntryRepository entries;
    UserRepository users;
    AuditService audit;
    DirectSaleController controller;
    Customer customer;
    Sale sale;
    @BeforeEach void setup() {
        customers = mock(CustomerRepository.class); sales = mock(SaleRepository.class); saleService = mock(SaleService.class);
        entries = mock(MoneyEntryRepository.class); users = mock(UserRepository.class); audit = mock(AuditService.class);
        controller = new DirectSaleController(customers, sales, saleService, mock(MoneyService.class), entries, mock(DatabaseConstraintRepair.class), users, audit);
        customer = Customer.builder().id(7L).customerType("DIRECT").customerName("Customer A").phoneNumber("+91 98765 43210").email("a@example.com").build();
        sale = Sale.builder().id(10L).customer(customer).quantity(1.0).totalAmount(250.0).amountCollected(250.0).pendingAmount(0.0).collectorName("Santosh").build();
        when(saleService.createSale(any(), any())).thenReturn(SaleResponse.builder().id(10L).build());
        when(sales.findById(10L)).thenReturn(Optional.of(sale));
        when(customers.findById(7L)).thenReturn(Optional.of(customer));
        when(customers.save(any())).thenAnswer(i -> { Customer c = i.getArgument(0); c.setId(7L); return c; });
    }
    DirectSaleController.Request request(Long customerId, String phone, String email, Long collector) {
        return new DirectSaleController.Request("request-1", customerId, "Customer A", phone, email, "Bengaluru", "Farm gate", "Mushrooms",
                BigDecimal.ONE, new BigDecimal("250"), new BigDecimal("250"), "Test sale", collector,null,null);
    }
    UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken(User.builder().id(1L).name("Santosh").build(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
    @Test void historyIncludesEditableAttributesAndFingerprint(){sale.setUnitPrice(250d);sale.setSalesChannel("DIRECT");sale.setRemarks("Notes");sale.setCollectorUserId(2L);when(sales.findAll()).thenReturn(List.of(sale));var rows=(List<DirectSaleController.SaleView>)controller.history().getData();assertEquals(250d,rows.getFirst().unitPrice());assertEquals("Notes",rows.getFirst().remarks());assertNotNull(rows.getFirst().fingerprint());}
    @Test void stalePurchaseIsRejectedBeforeMoneyChanges(){sale.setSalesChannel("DIRECT");var old=request(7L,null,null,2L);var r=new DirectSaleController.Request("edit",7L,old.customerName(),null,null,null,old.purchaseLocation(),old.productName(),old.quantity(),old.unitPrice(),old.amountCollected(),old.remarks(),2L,java.time.LocalDate.of(2026,1,1),"stale");assertThrows(IllegalArgumentException.class,()->controller.edit(10L,r,auth("SUPER_ADMIN")));verifyNoInteractions(saleService);}
    @Test void adminCollectorIsPassedToSaleServiceAndResponseIncludesCustomerAndSale() {
        when(users.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).active(true).build()));
        var result = controller.create(request(7L, null, null, 2L), auth("SUPER_ADMIN"));
        var payload = (DirectSaleController.SavedSale) result.getData();
        assertEquals(7L, payload.customer().id()); assertEquals(10L, payload.sale().id());
        verify(saleService).createSale(argThat(r -> r.isDirectSale() && r.getCollectorUserId().equals(2L)), any());
        verify(audit).createAudit(any(), eq("DIRECT_SALE"), eq("CREATE_DIRECT_SALE"), eq("10"), contains("collector"));
    }
    @Test void nonAdminCannotForgeCollector() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> controller.create(request(7L,null,null,2L), auth("SALES_EMPLOYEE")));
        verifyNoInteractions(saleService);
    }
    @Test void existingPhoneWithFormattingReusesCustomerInsteadOfCreatingDuplicate() {
        when(customers.findAll()).thenReturn(List.of(customer));
        controller.create(request(null,"9876543210",null,null), auth("SALES_EMPLOYEE"));
        verify(customers, never()).save(any());
        verify(saleService).createSale(argThat(r -> r.getCustomerId().equals(7L) && r.getCollectorUserId() == null), any());
    }
    @Test void existingEmailIsMatchedCaseInsensitively() {
        when(customers.findAll()).thenReturn(List.of(customer));
        controller.create(request(null,null,"A@EXAMPLE.COM",null), auth("SALES_EMPLOYEE"));
        verify(customers, never()).save(any());
    }
    @Test void newCustomerIsSavedAndAudited() {
        controller.create(request(null,"9000000000","new@example.com",null), auth("SALES_EMPLOYEE"));
        verify(customers).save(argThat(c -> "DIRECT".equals(c.getCustomerType()) && "new@example.com".equals(c.getEmail())));
        verify(audit).createAudit(any(), eq("CUSTOMER"), eq("CREATE_DIRECT_CUSTOMER"), eq("7"), contains("Customer A"));
    }
    @Test void duplicateSubmissionReturnsSameSavedPayloadWithoutAnotherSale() {
        var request = request(7L,null,null,null);
        var marker = new MoneyEntry(); marker.setSaleId(10L); marker.setRequestFingerprint(MoneyService.fingerprint(request));
        when(entries.findByRequestKey("direct:request-1")).thenReturn(Optional.of(marker));
        var result = controller.create(request, auth("SALES_EMPLOYEE"));
        assertInstanceOf(DirectSaleController.SavedSale.class, result.getData()); verifyNoInteractions(saleService);
    }
}
