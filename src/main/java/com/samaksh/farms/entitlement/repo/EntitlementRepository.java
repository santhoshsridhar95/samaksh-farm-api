package com.samaksh.farms.entitlement.repo;

import com.samaksh.farms.entitlement.entity.Entitlement;
import com.samaksh.farms.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntitlementRepository
        extends JpaRepository<Entitlement, Long> {

    List<Entitlement> findByRoleAndEnabledTrue(
            Role role
    );

    boolean existsByRoleAndPermissionKey(
            Role role,
            String permissionKey
    );
}
