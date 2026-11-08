package com.samaksh.farms.inventory;
import com.samaksh.farms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/inventory-options") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','FARM_MANAGER','LABOUR')")
public class InventoryOptionsController {
    private final InventoryCatalogService service;
    @GetMapping public ApiResponse<?> list() { return ok(service.list()); }
    @PostMapping @PreAuthorize("hasAnyRole('SUPER_ADMIN','FARM_MANAGER')") public ApiResponse<?> create(@Valid @RequestBody InventoryCatalogService.OptionRequest r, Authentication a) { return ok(service.save(null,r,a)); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')") public ApiResponse<?> edit(@PathVariable String id, @Valid @RequestBody InventoryCatalogService.OptionRequest r, Authentication a) { return ok(service.save(id,r,a)); }
    private ApiResponse<Object> ok(Object data) { return ApiResponse.builder().success(true).data(data).build(); }
}
