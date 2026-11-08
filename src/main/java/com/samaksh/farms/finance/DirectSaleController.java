package com.samaksh.farms.finance;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.sale.dto.SaleRequest;
import com.samaksh.farms.sale.service.SaleService;
import com.samaksh.farms.sale.repo.SaleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController @RequestMapping("/api/direct-sales") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','SALES_ADMIN','SALES_EMPLOYEE','SALES_USER')")
public class DirectSaleController {
    private final CustomerRepository customers;
    private final SaleRepository sales;
    private final SaleService saleService;
    private final MoneyService moneyService;
    private final MoneyEntryRepository entries;
    private final com.samaksh.farms.config.DatabaseConstraintRepair constraintRepair;
    private final com.samaksh.farms.user.repo.UserRepository users;
    private final com.samaksh.farms.audit.service.AuditService audit;

    public record Request(@NotBlank @Size(max=80) String requestKey, Long customerId,
            @Size(max=150) String customerName, @Size(max=30) String phoneNumber,
            @Email @Size(max=150) String email, @Size(max=250) String address,
            @NotBlank @Size(max=150) String purchaseLocation,
            @NotBlank @Size(max=150) String productName,
            @NotNull @DecimalMin("0.01") @Digits(integer=6,fraction=3) BigDecimal quantity,
            @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal unitPrice,
            @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal amountCollected,
            @Size(max=500) String remarks, Long collectorUserId) {}

    @GetMapping("/customers") @Transactional(readOnly=true)
    public ApiResponse<?> customers() {
        return ok(customers.findAll().stream().filter(c -> "DIRECT".equals(c.getCustomerType())).map(this::customerView).toList());
    }

    @GetMapping @Transactional(readOnly=true)
    public ApiResponse<?> history() {
        return ok(sales.findAll().stream().filter(s -> "DIRECT".equals(s.getSalesChannel()))
                .sorted(java.util.Comparator.comparing(com.samaksh.farms.sale.entity.Sale::getId).reversed())
                .map(this::saleView).toList());
    }

    @PostMapping @Transactional
    public ApiResponse<?> create(@Valid @RequestBody Request r, Authentication auth) {
        if (r.collectorUserId() != null) {
            boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
            if (!admin) throw new org.springframework.security.access.AccessDeniedException("Only admin can choose who collected the money");
            var collector = users.findById(r.collectorUserId()).orElseThrow(() -> new IllegalArgumentException("Collector not found"));
            if (!Boolean.TRUE.equals(collector.getActive())) throw new IllegalArgumentException("Choose an active collector");
        }
        moneyService.prepare();
        var prior = entries.findByRequestKey("direct:" + r.requestKey());
        if (prior.isPresent()) {
            if (!java.util.Objects.equals(prior.get().getRequestFingerprint(), MoneyService.fingerprint(r)))
                throw new IllegalArgumentException("This sale was already saved with different details. Refresh before recording another sale.");
            var saved = sales.findById(prior.get().getSaleId()).orElseThrow();
            return ok(new SavedSale(customerView(saved.getCustomer()), saleView(saved)));
        }
        if (r.amountCollected().compareTo(r.quantity().multiply(r.unitPrice())) > 0)
            throw new IllegalArgumentException("Collection cannot exceed the direct sale amount");
        Customer customer;
        if (r.customerId() != null) {
            customer = customers.findById(r.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            if (!"DIRECT".equals(customer.getCustomerType())) throw new IllegalArgumentException("Choose a direct customer");
        } else {
            if (r.customerName() == null || r.customerName().isBlank()) throw new IllegalArgumentException("Customer name is required");
            String phone = optional(r.phoneNumber());
            String email = optional(r.email());
            var matches = customers.findAll().stream().filter(c -> "DIRECT".equals(c.getCustomerType()))
                    .filter(c -> (phone != null && !normalizePhone(phone).isEmpty() && normalizePhone(phone).equals(normalizePhone(c.getPhoneNumber())))
                            || (email != null && email.equalsIgnoreCase(c.getEmail())))
                    .toList();
            if (matches.size() > 1) throw new IllegalArgumentException("These contact details match multiple customers. Select the correct customer from the search results.");
            if (matches.size() == 1) {
                customer = matches.getFirst();
            } else {
                constraintRepair.repairCustomerOptionalContactConstraints();
                customer = new Customer();
                customer.setCustomerName(r.customerName().trim()); customer.setPhoneNumber(phone);
                customer.setEmail(email); customer.setAddress(optional(r.address())); customer.setLocation(r.purchaseLocation());
                customer.setCustomerType("DIRECT"); customer.setShopCategory("Direct customer");
                customer.setActive(true); customer.setCreatedAt(BusinessTime.now());
                customer = customers.save(customer);
                audit.createAudit(auth, "CUSTOMER", "CREATE_DIRECT_CUSTOMER", customer.getId().toString(),
                        "Created direct customer " + customer.getCustomerName() + ", phone: " + customer.getPhoneNumber() + ", email: " + customer.getEmail());
            }
        }
        var request = new SaleRequest();
        request.setCustomerId(customer.getId()); request.setProductName(r.productName());
        request.setQuantity(r.quantity().doubleValue()); request.setUnitPrice(r.unitPrice().doubleValue());
        request.setAmountCollected(r.amountCollected().doubleValue()); request.setRemarks(r.remarks());
        request.setCollectorUserId(r.collectorUserId());
        // Direct-sale receipts belong to this purchase, not an older customer's invoice.
        request.setDirectSale(true);
        var response = saleService.createSale(request, auth);
        var sale = sales.findById(response.getId()).orElseThrow();
        sale.setSalesChannel("DIRECT"); sale.setPurchaseLocation(r.purchaseLocation().trim());
        sales.save(sale);
        var marker = new MoneyEntry();
        marker.setRequestKey("direct:" + r.requestKey()); marker.setType("DIRECT_SALE_RECORD");
        marker.setRequestFingerprint(MoneyService.fingerprint(r));
        marker.setAmount(BigDecimal.ZERO); marker.setSaleId(sale.getId()); marker.setDate(BusinessTime.today());
        marker.setRecordedAt(BusinessTime.now()); marker.setRecordedBy(auth.getName());
        marker.setDescription("Direct sale #" + sale.getId()); entries.save(marker);
        audit.createAudit(auth, "DIRECT_SALE", "CREATE_DIRECT_SALE", sale.getId().toString(),
                "Customer: " + customer.getCustomerName() + ", location: " + sale.getPurchaseLocation() + ", total Rs. " + sale.getTotalAmount()
                        + ", collected Rs. " + sale.getAmountCollected() + ", collector: " + sale.getCollectorName());
        return ok(new SavedSale(customerView(customer), saleView(sale)));
    }
    private ApiResponse<Object> ok(Object data) { return ApiResponse.builder().success(true).data(data).build(); }
    private String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    static String normalizePhone(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        return digits.length() == 12 && digits.startsWith("91") ? digits.substring(2) : digits;
    }
    private CustomerView customerView(Customer c) { return new CustomerView(c.getId(), c.getCustomerName(), c.getPhoneNumber(), c.getEmail(), c.getAddress()); }
    private SaleView saleView(com.samaksh.farms.sale.entity.Sale s) {
        return new SaleView(s.getId(), s.getCustomer().getId(), s.getCustomer().getCustomerName(), s.getProduct() == null ? "Direct purchase" : s.getProduct().getProductName(),
                s.getQuantity(), s.getTotalAmount(), s.getAmountCollected(), s.getPendingAmount(), s.getPurchaseLocation(), s.getSaleDate(), s.getCollectorName());
    }
    public record SavedSale(CustomerView customer, SaleView sale) {}
    public record CustomerView(Long id, String customerName, String phoneNumber, String email, String address) {}
    public record SaleView(Long id, Long customerId, String customerName, String productName, Double quantity, Double totalAmount,
                           Double amountCollected, Double pendingAmount, String purchaseLocation, java.time.LocalDateTime saleDate, String collectorName) {}
}
