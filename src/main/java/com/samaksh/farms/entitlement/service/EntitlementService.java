package com.samaksh.farms.entitlement.service;

import com.samaksh.farms.entitlement.dto.EntitlementResponse;
import com.samaksh.farms.entitlement.entity.Entitlement;
import com.samaksh.farms.entitlement.repo.EntitlementRepository;
import com.samaksh.farms.enums.Role;
import com.samaksh.farms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EntitlementService {

    public static final String MANAGE_SHOPS = "sales.manage_shops";
    public static final String CREATE_DELIVERY = "sales.create_delivery";
    public static final String VIEW_LEDGER = "sales.view_ledger";
    public static final String VIEW_CONFIDENTIAL = "sales.view_confidential";
    public static final String MANAGE_USERS = "users.manage";

    private final EntitlementRepository entitlementRepository;

    public EntitlementResponse getCurrentUserEntitlements(
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return EntitlementResponse.builder()
                .role(user.getRole())
                .permissions(
                        permissionsForUser(user)
                )
                .build();
    }

    public List<String> permissionsForUser(
            User user
    ) {

        Set<String> permissions =
                new LinkedHashSet<>(
                        permissionsForRole(user.getRole())
                );

        if (user.getExtraRoles() != null) {
            user.getExtraRoles()
                    .forEach(role -> permissions.addAll(permissionsForRole(role)));
        }

        if (user.getExtraPermissions() != null) {
            permissions.addAll(user.getExtraPermissions());
        }

        return List.copyOf(permissions);
    }

    public List<String> permissionsForRole(
            Role role
    ) {

        if (role == Role.SUPER_ADMIN) {

            return List.of(
                    MANAGE_SHOPS,
                    CREATE_DELIVERY,
                    VIEW_LEDGER,
                    VIEW_CONFIDENTIAL,
                    MANAGE_USERS
            );
        }

        return entitlementRepository.findByRoleAndEnabledTrue(role)
                .stream()
                .map(Entitlement::getPermissionKey)
                .toList();
    }
}
