package com.samaksh.farms.customer.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Objects;

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
                        .location(
                                defaultLocation(
                                        request.getLocation()
                                )
                        )
                        .shopCategory(
                                request.getShopCategory()
                        )
                        .products(
                                cleanProducts(
                                        request.getProducts()
                                )
                        )
                        .minimumBoxesPerDay(
                                request.getMinimumBoxesPerDay()
                        )
                        .dailyReturnedBoxes(
                                request.getDailyReturnedBoxes()
                        )
                        .defaultBoxPrice(
                                request.getDefaultBoxPrice()
                        )
                        .shopkeeperSellingPrice(
                                request.getShopkeeperSellingPrice()
                        )
                        .exchangeType(
                                request.getExchangeType()
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

    public CustomerResponse updateCustomer(
            Long id,
            CustomerRequest request,
            Authentication authentication
    ) {

        Customer customer =
                customerRepository.findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Customer",
                                        id
                                )
                        );

        customer.setCustomerName(
                request.getCustomerName()
        );
        customer.setContactPerson(
                request.getContactPerson()
        );
        customer.setPhoneNumber(
                request.getPhoneNumber()
        );
        customer.setEmail(
                request.getEmail()
        );
        customer.setAddress(
                request.getAddress()
        );
        customer.setLocation(
                defaultLocation(
                        request.getLocation()
                )
        );
        customer.setShopCategory(
                request.getShopCategory()
        );
        customer.setProducts(
                cleanProducts(
                        request.getProducts()
                )
        );
        customer.setMinimumBoxesPerDay(
                request.getMinimumBoxesPerDay()
        );
        customer.setDailyReturnedBoxes(
                request.getDailyReturnedBoxes()
        );
        customer.setDefaultBoxPrice(
                request.getDefaultBoxPrice()
        );
        customer.setShopkeeperSellingPrice(
                request.getShopkeeperSellingPrice()
        );
        customer.setExchangeType(
                request.getExchangeType()
        );
        customer.setActive(
                request.getActive() == null
                        ? true
                        : request.getActive()
        );

        Customer savedCustomer =
                customerRepository.save(customer);

        auditService.createAudit(
                authentication,
                "CUSTOMER",
                "UPDATE_CUSTOMER",
                savedCustomer.getCustomerName(),
                "Customer updated"
        );

        return mapToResponse(savedCustomer);
    }

    public PagedResponse<CustomerResponse> getCustomers(
            int page,
            int size,
            String search,
            boolean includeInactive
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("customerName")
                                .ascending()
                );

        Page<Customer> customers =
                customerRepository.searchCustomers(
                        search,
                        includeInactive,
                        pageable
                );

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
                .location(
                        defaultLocation(
                                customer.getLocation()
                        )
                )
                .shopCategory(
                        customer.getShopCategory()
                )
                .products(
                        cleanProducts(
                                customer.getProducts()
                        )
                )
                .minimumBoxesPerDay(
                        customer.getMinimumBoxesPerDay()
                )
                .dailyReturnedBoxes(
                        customer.getDailyReturnedBoxes()
                )
                .defaultBoxPrice(
                        customer.getDefaultBoxPrice()
                )
                .shopkeeperSellingPrice(
                        customer.getShopkeeperSellingPrice()
                )
                .exchangeType(
                        customer.getExchangeType()
                )
                .active(
                        customer.getActive()
                )
                .build();
    }

    public CustomerResponse softDeleteCustomer(
            Long id,
            Authentication authentication
    ) {

        Customer customer =
                customerRepository.findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Customer",
                                        id
                                )
                        );

        customer.setActive(false);

        Customer savedCustomer =
                customerRepository.save(customer);

        auditService.createAudit(
                authentication,
                "CUSTOMER",
                "DELETE_CUSTOMER",
                savedCustomer.getCustomerName(),
                "Shop soft deleted"
        );

        return mapToResponse(savedCustomer);
    }

    private String defaultLocation(
            String location
    ) {

        if (location == null ||
                location.isBlank()) {

            return "R.T. Nagar";
        }

        return location;
    }

    private List<String> cleanProducts(
            List<String> products
    ) {

        if (products == null) {
            return List.of();
        }

        return products.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(product -> !product.isBlank())
                .distinct()
                .toList();
    }
}
