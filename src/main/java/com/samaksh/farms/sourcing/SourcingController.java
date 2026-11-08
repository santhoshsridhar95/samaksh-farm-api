package com.samaksh.farms.sourcing;
import com.samaksh.farms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/sourcing") @RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SourcingController {
    private final SourcingService service;
    private final SourcingReportService reports;
    @GetMapping("/report") @PreAuthorize("hasRole('SUPER_ADMIN')") public ApiResponse<?> report(@RequestParam java.time.LocalDate from,@RequestParam java.time.LocalDate to){return ok(reports.report(from,to));}
    @GetMapping public ApiResponse<?> dashboard() { return ok(service.dashboard()); }
    @PostMapping("/vendors") public ApiResponse<?> vendor(@Valid @RequestBody SourcingRequests.Vendor r, Authentication a) { return ok(service.vendor(null,r,a)); }
    @PutMapping("/vendors/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')") public ApiResponse<?> editVendor(@PathVariable Long id, @Valid @RequestBody SourcingRequests.Vendor r, Authentication a) { return ok(service.vendor(id,r,a)); }
    @PostMapping("/receipts") public ApiResponse<?> receipt(@Valid @RequestBody SourcingRequests.Receipt r, Authentication a) { return ok(service.receipt(null,r,a)); }
    @PutMapping("/receipts/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')") public ApiResponse<?> editReceipt(@PathVariable Long id, @Valid @RequestBody SourcingRequests.Receipt r, Authentication a) { return ok(service.receipt(id,r,a)); }
    @PostMapping("/payments") public ApiResponse<?> payment(@Valid @RequestBody SourcingRequests.Payment r, Authentication a) { return ok(service.payment(null,r,a)); }
    @PutMapping("/payments/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')") public ApiResponse<?> editPayment(@PathVariable Long id, @Valid @RequestBody SourcingRequests.Payment r, Authentication a) { return ok(service.payment(id,r,a)); }
    @PostMapping("/recoveries") public ApiResponse<?> recovery(@Valid @RequestBody SourcingRequests.Recovery r, Authentication a) { return ok(service.recovery(null,r,a)); }
    @PutMapping("/recoveries/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')") public ApiResponse<?> editRecovery(@PathVariable Long id,@Valid @RequestBody SourcingRequests.Recovery r,Authentication a){return ok(service.recovery(id,r,a));}
    @PutMapping("/price") @PreAuthorize("hasRole('SUPER_ADMIN')") public ApiResponse<?> price(@Valid @RequestBody SourcingRequests.Price r, Authentication a) { return ok(service.price(r,a)); }
    private ApiResponse<Object> ok(Object data) { return ApiResponse.builder().success(true).data(data).build(); }
}
