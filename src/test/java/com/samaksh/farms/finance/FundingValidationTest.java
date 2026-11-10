package com.samaksh.farms.finance;

import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.sourcing.SourcingRequests;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FundingValidationTest {
    @Test void splitRequestsAcceptPastDatesAndRejectFutureDatesAndInvalidNestedAllocations() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var amount = new BigDecimal("100.00");
            var sources = List.of(new FundingAllocation(4L, amount));
            for (var date : List.of(BusinessTime.today().minusYears(1), BusinessTime.today().plusDays(1))) {
                var expense = new MoneyService.SplitRequest(new MoneyRequest("key", "EXPENSE", 4L, null, amount, "Expense", null, null, date), sources);
                var vendor = new SourcingRequests.SplitPayment(new SourcingRequests.Payment("key", 2L, 4L, amount, date, "Payment"), sources);
                assertEquals(date.isAfter(BusinessTime.today()), !validator.validate(expense).isEmpty());
                assertEquals(date.isAfter(BusinessTime.today()), !validator.validate(vendor).isEmpty());
            }
            var invalid = new MoneyService.SplitRequest(new MoneyRequest("key", "EXPENSE", 4L, null, amount, "Expense", null, null, BusinessTime.today()),
                    List.of(new FundingAllocation(4L, new BigDecimal("-1"))));
            assertFalse(validator.validate(invalid).isEmpty());
        }
    }
}
