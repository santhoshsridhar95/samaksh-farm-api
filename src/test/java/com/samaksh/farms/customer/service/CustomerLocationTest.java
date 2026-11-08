package com.samaksh.farms.customer.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.config.DatabaseConstraintRepair;
import com.samaksh.farms.customer.dto.CustomerRequest;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerLocationTest {
    final CustomerRepository repository = mock(CustomerRepository.class);
    final CustomerService service = new CustomerService(repository, mock(AuditService.class), mock(DatabaseConstraintRepair.class));
    CustomerRequest request() {
        CustomerRequest r = new CustomerRequest();
        r.setCustomerName("Shop"); r.setShopCategory("Vegetable Shop");
        r.setMapLocation("12.97,77.59"); r.setShopStage("PROSPECTIVE"); return r;
    }
    @Test void createAndEditKeepLocationAndProspectStatus() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.createCustomer(request(), null);
        assertEquals("12.97,77.59", result.getMapLocation());
        assertEquals("PROSPECTIVE", result.getShopStage());
        Customer existing = Customer.builder().id(1L).shopStage("EXISTING").build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        var edit = request(); edit.setShopStage("RETURN_PROSPECTIVE"); edit.setMapLocation("https://maps.app.goo.gl/example");
        result = service.updateCustomer(1L, edit, null);
        assertEquals(edit.getMapLocation(), result.getMapLocation());
        assertEquals("RETURN_PROSPECTIVE", result.getShopStage());
        edit.setMapLocation(null);
        assertNull(service.updateCustomer(1L, edit, null).getMapLocation());
    }
    @Test void rejectsInvalidStageAndOversizedLocationBeforeWriting() {
        var r = request(); r.setShopStage("UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> service.createCustomer(r, null));
        r.setShopStage("EXISTING"); r.setMapLocation("x".repeat(2049));
        assertThrows(IllegalArgumentException.class, () -> service.createCustomer(r, null));
        verifyNoInteractions(repository);
    }
}
