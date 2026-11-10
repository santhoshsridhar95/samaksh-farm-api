package com.samaksh.farms.sourcing;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.samaksh.farms.finance.FundingAllocation;
public class SourcingRequests {
    public record Vendor(@NotBlank @Size(max=70) String requestKey, @NotBlank @Size(max=150) String name,
        @Pattern(regexp="VENDOR|OWN_FARM") @NotNull String kind, @Size(max=30) String phone, @Size(max=250) String address,
        @NotNull @DecimalMin("0.01") @Digits(integer=6,fraction=4) BigDecimal boxesPerKg,
        @NotNull @DecimalMin("0") @Digits(integer=8,fraction=4) BigDecimal costPerKg,
        @NotNull @Pattern(regexp="VENDOR|FARM") String expensesBorneBy, boolean active) {}
    public record Receipt(@NotBlank @Size(max=70) String requestKey, @NotNull Long vendorId,
        @NotNull @PastOrPresent LocalDate date, @NotBlank @Size(max=150) String product,
        @NotNull @Pattern(regexp="KG|BOX") String unit,
        @NotNull @DecimalMin("0.000001") @Digits(integer=6,fraction=6) BigDecimal quantity,
        @NotNull @DecimalMin("0") @Digits(integer=8,fraction=4) BigDecimal costPerKg,
        @NotNull @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal transportCost,
        @NotNull @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal otherCost,
        @NotNull @Pattern(regexp="VENDOR|FARM") String expensesBorneBy,
        @NotNull @Pattern(regexp="VENDOR|FARM") String expensesPaidBy, Long expenseAccountId,
        @NotNull @DecimalMin("0") @Digits(integer=6,fraction=6) BigDecimal soldKg,
        @Size(max=1000) String notes,
        @DecimalMin("0") @Digits(integer=6,fraction=3) BigDecimal damagedBoxes, LocalDate plannedSellingDate) {}
    public record Payment(@NotBlank @Size(max=70) String requestKey, @NotNull Long receiptId,
        @NotNull Long sourceAccountId, @NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal amount,
        @NotNull @PastOrPresent LocalDate date, @Size(max=500) String notes,
        @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal additionalAmount) {
        public Payment(String key,Long receipt,Long source,BigDecimal amount,LocalDate date,String notes){this(key,receipt,source,amount,date,notes,BigDecimal.ZERO);}
    }
    public record Recovery(@NotBlank @Size(max=70) String requestKey, @NotNull Long paymentId, @NotNull Long destinationAccountId,
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount, @NotNull @PastOrPresent LocalDate date, @Size(max=500) String notes) {}
    public record SplitPayment(@jakarta.validation.Valid @NotNull Payment payment,
        @NotEmpty @Size(max=20) java.util.List<@jakarta.validation.Valid FundingAllocation> sources) {}
    public record Price(@NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=4) BigDecimal sellingPricePerKg) {}
}
