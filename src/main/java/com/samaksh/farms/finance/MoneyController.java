package com.samaksh.farms.finance;
import com.samaksh.farms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/money") @RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class MoneyController {
    private final MoneyService service;
    @GetMapping public ApiResponse<?> dashboard() { return ok(service.dashboard()); }
    @PostMapping("/accounts") public ApiResponse<?> account(@RequestBody Map<String,String> r) { return ok(service.createAccount(r.get("name"), r.get("kind"))); }
    @PostMapping("/entries") public ApiResponse<?> record(@Valid @RequestBody MoneyRequest r, Authentication auth) { return ok(service.record(r, auth)); }
    @PutMapping("/entries/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<?> edit(@PathVariable Long id, @Valid @RequestBody MoneyService.EditRequest r, Authentication auth) { return ok(service.edit(id, r, auth)); }
    @PostMapping("/settle") public ApiResponse<?> settle(@Valid @RequestBody MoneyService.SettlementRequest r, Authentication auth) { return ok(service.settle(r, auth)); }
    @PostMapping("/reconcile") public ApiResponse<?> reconcile(@Valid @RequestBody MoneyService.ReconciliationRequest r, Authentication auth) { return ok(service.reconcile(r, auth)); }
    @PostMapping("/entries/{id}/reverse") public ApiResponse<?> reverse(@PathVariable Long id, @RequestBody Map<String,String> r, Authentication auth) { return ok(service.reverse(id, r.get("reason"), auth)); }
    private ApiResponse<Object> ok(Object data) { return ApiResponse.builder().success(true).data(data).build(); }
}
