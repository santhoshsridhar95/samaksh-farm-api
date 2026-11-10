package com.samaksh.farms.employee;
import com.samaksh.farms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/employees") @RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class EmployeeController {
 private final EmployeeService service;
 @GetMapping public ApiResponse<?> list(){return ok(service.list());}
 @PostMapping public ApiResponse<?> create(@Valid @RequestBody EmployeeService.ProfileRequest r,Authentication a){return ok(service.save(null,r,a));}
 @PutMapping("/{id}") public ApiResponse<?> edit(@PathVariable Long id,@Valid @RequestBody EmployeeService.ProfileRequest r,Authentication a){return ok(service.save(id,r,a));}
 @GetMapping("/{id}/report") public ApiResponse<?> report(@PathVariable Long id,@RequestParam LocalDate from,@RequestParam LocalDate to){return ok(service.report(id,from,to));}
 @PostMapping("/{id}/work") public ApiResponse<?> work(@PathVariable Long id,@Valid @RequestBody EmployeeService.WorkRequest r,Authentication a){return ok(service.work(id,r,a));}
 @PostMapping("/{id}/work/{workId}/void") public ApiResponse<?> voidWork(@PathVariable Long id,@PathVariable Long workId,Authentication a){return ok(service.voidWork(id,workId,a));}
 private ApiResponse<Object> ok(Object data){return ApiResponse.builder().success(true).data(data).build();}
}
