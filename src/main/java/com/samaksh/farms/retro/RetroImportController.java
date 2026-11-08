package com.samaksh.farms.retro;
import com.samaksh.farms.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@RestController @RequestMapping("/api/retro-import") @RequiredArgsConstructor @PreAuthorize("hasRole('SUPER_ADMIN')")
public class RetroImportController {
 private final RetroImportService service;
 private final RetroSpreadsheet spreadsheet;
 @GetMapping("/modules") public ApiResponse<?> modules(){return ok(service.modules());}
 @GetMapping("/{module}/schema") public ApiResponse<?> schema(@PathVariable String module){return ok(service.schema(module));}
 @GetMapping("/history") public ApiResponse<?> history(){return ok(service.history());}
 @PostMapping("/{module}/file") public ApiResponse<?> file(@PathVariable String module,@RequestParam("file") MultipartFile file)throws java.io.IOException {var rows=spreadsheet.read(file.getOriginalFilename()==null?"data.xlsx":file.getOriginalFilename(),file.getBytes());return ok(Map.of("rows",rows,"results",service.preview(module,rows)));}
 public record Rows(List<Map<String,String>> rows){}
 @PostMapping("/{module}/preview") public ApiResponse<?> preview(@PathVariable String module,@RequestBody Rows body){return ok(service.preview(module,body.rows()));}
 @PostMapping("/{module}/commit") public ApiResponse<?> commit(@PathVariable String module,@RequestBody Rows body,Authentication auth){return ok(service.commit(module,body.rows(),auth));}
 private ApiResponse<Object> ok(Object data){return ApiResponse.builder().success(true).data(data).build();}
}
