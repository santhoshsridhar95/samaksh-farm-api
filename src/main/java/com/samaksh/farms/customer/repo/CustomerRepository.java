package com.samaksh.farms.customer.repo;

import com.samaksh.farms.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    Page<Customer>
    findByCustomerNameContainingIgnoreCaseOrContactPersonContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(
            String customerName,
            String contactPerson,
            String phoneNumber,
            Pageable pageable
    );

    @Query("""
            select customer
            from Customer customer
            where (:includeInactive = true or customer.active = true)
              and (
                    :search is null
                    or :search = ''
                    or lower(customer.customerName) like lower(concat('%', :search, '%'))
                    or lower(customer.contactPerson) like lower(concat('%', :search, '%'))
                    or lower(customer.phoneNumber) like lower(concat('%', :search, '%'))
                    or lower(customer.location) like lower(concat('%', :search, '%'))
                    or lower(customer.shopCategory) like lower(concat('%', :search, '%'))
              )
            """)
    Page<Customer> searchCustomers(
            @Param("search") String search,
            @Param("includeInactive") boolean includeInactive,
            Pageable pageable
    );
}
