package com.samaksh.farms.finance;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record MoneyRequest(
    @NotBlank @Size(max = 80) String requestKey,
    @NotBlank String type, Long sourceId, Long destinationId,
    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
    @NotBlank @Size(max = 1000) String description,
    @Size(max = 150) String payee, String salaryMonth, @NotNull @PastOrPresent LocalDate date, Long employeeId
) {
 public MoneyRequest(String requestKey,String type,Long sourceId,Long destinationId,BigDecimal amount,String description,String payee,String salaryMonth,LocalDate date){this(requestKey,type,sourceId,destinationId,amount,description,payee,salaryMonth,date,null);}
}
