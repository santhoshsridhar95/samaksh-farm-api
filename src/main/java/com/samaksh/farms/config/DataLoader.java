package com.samaksh.farms.config;

import com.samaksh.farms.enums.Role;
import com.samaksh.farms.enums.ApprovalStatus;
import com.samaksh.farms.entitlement.entity.Entitlement;
import com.samaksh.farms.entitlement.repo.EntitlementRepository;
import com.samaksh.farms.entitlement.service.EntitlementService;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataLoader.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final EntitlementRepository entitlementRepository;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.name:Samaksh Farms Admin}")
    private String adminName;

    public DataLoader(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EntitlementRepository entitlementRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.entitlementRepository = entitlementRepository;
    }

    @Override
    public void run(String... args) {

        if (userRepository.count() == 0) {

            if (adminEmail == null
                    || adminEmail.isBlank()
                    || adminPassword == null
                    || adminPassword.isBlank()) {

                log.warn(
                        "No users exist. Configure app.bootstrap.admin.email and app.bootstrap.admin.password to bootstrap the first SUPER_ADMIN."
                );

                return;
            }

            User admin = new User();

            admin.setName(adminName);

            admin.setEmail(
                    adminEmail.trim()
                            .toLowerCase(Locale.ROOT)
            );

            admin.setPassword(
                    passwordEncoder.encode(adminPassword)
            );

            admin.setRole(Role.SUPER_ADMIN);

            admin.setActive(true);

            admin.setApprovalStatus(ApprovalStatus.APPROVED);

            admin.setApprovedAt(LocalDateTime.now());

            admin.setCreatedAt(
                    LocalDateTime.now()
            );

            userRepository.save(admin);

            log.info(
                    "Initial SUPER_ADMIN user created for {}",
                    admin.getEmail()
            );
        }

        seedEntitlement(
                Role.SALES_ADMIN,
                EntitlementService.MANAGE_SHOPS
        );
        seedEntitlement(
                Role.SALES_ADMIN,
                EntitlementService.CREATE_DELIVERY
        );
        seedEntitlement(
                Role.SALES_ADMIN,
                EntitlementService.VIEW_LEDGER
        );
        seedEntitlement(
                Role.SALES_ADMIN,
                EntitlementService.VIEW_CONFIDENTIAL
        );
        seedEntitlement(
                Role.SALES_EMPLOYEE,
                EntitlementService.CREATE_DELIVERY
        );
        seedEntitlement(
                Role.SALES_USER,
                EntitlementService.CREATE_DELIVERY
        );
        seedEntitlement(
                Role.SALES_USER,
                EntitlementService.VIEW_LEDGER
        );
    }

    private void seedEntitlement(
            Role role,
            String permissionKey
    ) {

        if (entitlementRepository.existsByRoleAndPermissionKey(
                role,
                permissionKey
        )) {

            return;
        }

        entitlementRepository.save(
                Entitlement.builder()
                        .role(role)
                        .permissionKey(permissionKey)
                        .enabled(true)
                        .build()
        );
    }
}
