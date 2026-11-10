package com.samaksh.farms.finance;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public record FundingAllocation(@NotNull Long accountId,
        @NotNull @DecimalMin("0.01") @Digits(integer=12, fraction=2) BigDecimal amount) {
    public static void validate(List<FundingAllocation> sources, BigDecimal total) {
        if (sources == null || sources.isEmpty() || sources.size() > 20)
            throw new IllegalArgumentException("Choose between 1 and 20 funding sources");
        var ids = new HashSet<Long>();
        var sum = BigDecimal.ZERO;
        for (var source : sources) {
            if (source == null || source.accountId() == null || !ids.add(source.accountId()))
                throw new IllegalArgumentException("Choose each funding account only once");
            if (source.amount() == null || source.amount().signum() <= 0 || source.amount().stripTrailingZeros().scale() > 2)
                throw new IllegalArgumentException("Enter a positive amount with at most two decimal places for each source");
            sum = sum.add(source.amount());
        }
        if (total == null || sum.compareTo(total) != 0)
            throw new IllegalArgumentException("Funding amounts must equal the total payment");
    }
}
