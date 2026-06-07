package com.samaksh.farms.customer.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.customer.dto.CustomerRequest;
import com.samaksh.farms.customer.dto.CustomerResponse;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    private final AuditService auditService;

    public CustomerResponse createCustomer(
            CustomerRequest request,
            Authentication authentication
    ) {

        Customer customer =
                Customer.builder()
                        .customerName(
                                request.getCustomerName()
                        )
                        .contactPerson(
                                request.getContactPerson()
                        )
                        .phoneNumber(
                                request.getPhoneNumber()
                        )
                        .email(
                                request.getEmail()
                        )
                        .address(
                                request.getAddress()
                        )
                        .active(true)
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        Customer savedCustomer =
                customerRepository.save(customer);

        auditService.createAudit(
                authentication,
                "CUSTOMER",
                "CREATE_CUSTOMER",
                savedCustomer.getCustomerName(),
                "Customer created"
        );

        return mapToResponse(savedCustomer);
    }

    public List<CustomerResponse> getCustomers() {

        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private CustomerResponse mapToResponse(
            Customer customer
    ) {

        return CustomerResponse.builder()
                .id(customer.getId())
                .customerName(customer.getCustomerName())
                .contactPerson(customer.getContactPerson())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .active(customer.getActive())
                .build();
    }
}