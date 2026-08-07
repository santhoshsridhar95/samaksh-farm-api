package com.samaksh.farms.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DatabaseConstraintRepair implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DatabaseConstraintRepair.class);

    private final JdbcTemplate jdbcTemplate;

    private final DataSource dataSource;

    @Override
    public void run(
            ApplicationArguments args
    ) throws Exception {

        try (Connection connection = dataSource.getConnection()) {
            String databaseProduct =
                    connection.getMetaData()
                            .getDatabaseProductName()
                            .toLowerCase(Locale.ROOT);

            if (!databaseProduct.contains("postgresql")) {
                return;
            }
        }

        repairUsersRoleConstraint();
        repairUsersApprovalStatusConstraint();
    }

    private void repairUsersRoleConstraint() {

        jdbcTemplate.execute(
                "ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check"
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE users
                ADD CONSTRAINT users_role_check
                CHECK (role IN (
                    'SUPER_ADMIN',
                    'FARM_MANAGER',
                    'SALES_ADMIN',
                    'SALES_EMPLOYEE',
                    'LABOUR',
                    'SALES_USER'
                ))
                """
        );

        LOGGER.info("Verified users_role_check constraint");
    }

    private void repairUsersApprovalStatusConstraint() {

        jdbcTemplate.execute(
                "ALTER TABLE users DROP CONSTRAINT IF EXISTS users_approval_status_check"
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE users
                ADD CONSTRAINT users_approval_status_check
                CHECK (approval_status IS NULL OR approval_status IN (
                    'PENDING',
                    'APPROVED',
                    'REJECTED',
                    'RESET_REQUESTED',
                    'DELETED'
                ))
                """
        );

        LOGGER.info("Verified users_approval_status_check constraint");
    }
}
