package com.samaksh.farms.sourcing;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class SourcingServiceTest {
    BigDecimal n(String value) { return new BigDecimal(value); }
    SourcingService.Costs costs(String borne, String paid) {
        return SourcingService.costs(n("10"), n("180"), n("250"), n("100"), borne, paid, false, n("6"));
    }
    @Test void vendorBorneTransportPaidByFarmReducesPayableNotMarginTwice() {
        var c = costs("VENDOR", "FARM");
        assertEquals(n("1800.00"), c.goodsCost());
        assertEquals(n("1700.00"), c.payable());
        assertEquals(n("100"), c.deduction());
        assertEquals(n("1800.00"), c.landedCost());
        assertEquals(n("700.00"), c.potentialMargin());
        assertEquals(n("420.00"), c.soldMargin());
    }
    @Test void farmBorneTransportPaidByFarmIsAnAdditionalCostNotADeduction() {
        var c = costs("FARM", "FARM");
        assertEquals(n("1800.00"), c.payable());
        assertEquals(n("1900.00"), c.landedCost());
        assertEquals(n("600.00"), c.potentialMargin());
        assertEquals(0, c.deduction().signum());
    }
    @Test void farmBorneTransportAdvancedByVendorIsReimbursed() {
        var c = costs("FARM", "VENDOR");
        assertEquals(n("1900.00"), c.payable());
        assertEquals(n("100"), c.reimbursement());
        assertEquals(n("1900.00"), c.landedCost());
    }
    @Test void vendorPaysOwnTransportWithoutChangingPayable() {
        var c = costs("VENDOR", "VENDOR");
        assertEquals(n("1800.00"), c.payable());
        assertEquals(n("700.00"), c.potentialMargin());
    }
    @Test void damagedStockReducesMarginButDoesNotSilentlyReduceVendorPayment() {
        var c = SourcingService.costs(n("10"),n("180"),n("250"),n("0"),"VENDOR","VENDOR",false,n("4"),n("2"));
        assertEquals(n("1800.00"),c.payable());
        assertEquals(n("2000.00"),c.potentialRevenue());
        assertEquals(n("200.00"),c.potentialMargin());
        assertEquals(n("100.00"),c.soldMargin());
    }
    @Test void ownFarmHasSeparateCostAndNoVendorPayable() {
        var c = SourcingService.costs(n("10"), n("100"), n("250"), n("0"), "FARM", "FARM", true, n("0"));
        assertEquals(0, c.payable().signum());
        assertEquals(n("1500.00"), c.potentialMargin());
        assertEquals(n("0.00"), c.soldMargin());
    }
}
