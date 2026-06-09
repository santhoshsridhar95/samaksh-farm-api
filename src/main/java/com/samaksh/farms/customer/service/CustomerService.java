package com.samaksh.farms.customer.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.customer.dto.CustomerRequest;
import com.samaksh.farms.customer.dto.CustomerResponse;
import com.samaksh.farms.customer.dto.PagedResponse;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    public PagedResponse<CustomerResponse> getCustomers(
            int page,
            int size,
            String search
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("customerName")
                                .ascending()
                );

        Page<Customer> customers;

        if (search != null &&
                !search.isBlank()) {

            customers =
                    customerRepository
                            .findByCustomerNameContainingIgnoreCaseOrContactPersonContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(
                                    search,
                                    search,
                                    search,
                                    pageable
                            );

        } else {

            customers =
                    customerRepository.findAll(
                            pageable
                    );
        }

        return PagedResponse
                .<CustomerResponse>builder()
                .content(
                        customers.getContent()
                                .stream()
                                .map(this::mapToResponse)
                                .toList()
                )
                .page(
                        customers.getNumber()
                )
                .size(
                        customers.getSize()
                )
                .totalElements(
                        customers.getTotalElements()
                )
                .totalPages(
                        customers.getTotalPages()
                )
                .last(
                        customers.isLast()
                )
                .build();
    }

    private CustomerResponse mapToResponse(
            Customer customer
    ) {

        return CustomerResponse.builder()
                .id(
                        customer.getId()
                )
                .customerName(
                        customer.getCustomerName()
                )
                .contactPerson(
                        customer.getContactPerson()
                )
                .phoneNumber(
                        customer.getPhoneNumber()
                )
                .email(
                        customer.getEmail()
                )
                .address(
                        customer.getAddress()
                )
                .active(
                        customer.getActive()
                )
                .build();
    }
}