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
import java.util.List;
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

        repairUserConstraints();
    }

    public void repairUserConstraints() {
        repairUsersRoleConstraint();
        repairUserExtraRolesConstraint();
        repairUsersApprovalStatusConstraint();
        repairCustomerOptionalContactConstraints();
        repairCustomerExchangeTypeConstraint();
        repairSalesExchangeTypeConstraint();
        repairSalesPaymentStatusConstraint();
        repairProductUnitTypeConstraint();
        repairOrderStatusConstraint();
    }

    private void repairUsersRoleConstraint() {

        dropUsersCheckConstraintsContaining("role");

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

    private void repairUserExtraRolesConstraint() {

        Boolean tableExists =
                jdbcTemplate.queryForObject(
                        "SELECT to_regclass('public.user_extra_roles') IS NOT NULL",
                        Boolean.class
                );

        if (!Boolean.TRUE.equals(tableExists)) {
            return;
        }

        List<String> checkConstraints =
                jdbcTemplate.queryForList(
                        """
                        SELECT conname
                        FROM pg_constraint
                        WHERE conrelid = 'public.user_extra_roles'::regclass
                        AND contype = 'c'
                        """,
                        String.class
                );

        for (String constraint : checkConstraints) {
            jdbcTemplate.execute(
                    "ALTER TABLE user_extra_roles DROP CONSTRAINT IF EXISTS " +
                            quoteIdentifier(constraint)
            );
        }

        jdbcTemplate.execute(
                """
                ALTER TABLE user_extra_roles
                ADD CONSTRAINT user_extra_roles_role_check
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

        LOGGER.info("Verified user_extra_roles_role_check constraint");
    }

    private void repairUsersApprovalStatusConstraint() {

        dropUsersCheckConstraintsContaining("approval_status");

        jdbcTemplate.execute(
                """
                ALTER TABLE users
                ADD CONSTRAINT users_approval_status_check
                CHECK (approval_status IS NULL OR approval_status IN (
                    'EMAIL_VERIFICATION_PENDING',
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

    private void dropUsersCheckConstraintsContaining(
            String columnName
    ) {

        Boolean tableExists =
                jdbcTemplate.queryForObject(
                        "SELECT to_regclass('public.users') IS NOT NULL",
                        Boolean.class
                );

        if (!Boolean.TRUE.equals(tableExists)) {
            return;
        }

        List<String> checkConstraints =
                jdbcTemplate.queryForList(
                        """
                        SELECT conname
                        FROM pg_constraint
                        WHERE conrelid = 'public.users'::regclass
                        AND contype = 'c'
                        AND lower(pg_get_constraintdef(oid)) LIKE ?
                        """,
                        String.class,
                        "%" + columnName.toLowerCase(Locale.ROOT) + "%"
                );

        for (String constraint : checkConstraints) {
            jdbcTemplate.execute(
                    "ALTER TABLE users DROP CONSTRAINT IF EXISTS " +
                            quoteIdentifier(constraint)
            );
        }
    }

    public void repairCustomerExchangeTypeConstraint() {

        repairEnumConstraint(
                "customers",
                "exchange_type",
                "customers_exchange_type_check",
                List.of(
                        "NONE",
                        "ONE_ON_ONE",
                        "TWO_ON_ONE"
                )
        );

        LOGGER.info("Verified customers_exchange_type_check constraint");
    }

    public void repairCustomerOptionalContactConstraints() {

        if (!tableExists("customers")) {
            return;
        }

        dropCustomerUniqueConstraintsContaining("email");
        dropCustomerUniqueConstraintsContaining("phone");
        dropCustomerUniqueConstraintsContaining("phone_number");
        dropCustomerUniqueIndexesContaining("email");
        dropCustomerUniqueIndexesContaining("phone");
        dropCustomerUniqueIndexesContaining("phone_number");

        dropCustomerColumnNotNull("contact_person");
        dropCustomerColumnNotNull("phone_number");
        dropCustomerColumnNotNull("email");
        dropCustomerColumnNotNull("address");

        LOGGER.info("Verified optional customer contact fields");
    }

    private void repairSalesExchangeTypeConstraint() {

        repairEnumConstraint(
                "sales",
                "exchange_type",
                "sales_exchange_type_check",
                List.of(
                        "NONE",
                        "ONE_ON_ONE",
                        "TWO_ON_ONE"
                )
        );

        LOGGER.info("Verified sales_exchange_type_check constraint");
    }

    private void repairSalesPaymentStatusConstraint() {

        repairEnumConstraint(
                "sales",
                "payment_status",
                "sales_payment_status_check",
                List.of(
                        "PAID",
                        "PENDING",
                        "PARTIAL"
                )
        );

        LOGGER.info("Verified sales_payment_status_check constraint");
    }

    private void repairProductUnitTypeConstraint() {

        repairEnumConstraint(
                "products",
                "unit_type",
                "products_unit_type_check",
                List.of(
                        "KG",
                        "BOX",
                        "PACK"
                )
        );

        LOGGER.info("Verified products_unit_type_check constraint");
    }

    private void repairOrderStatusConstraint() {

        repairEnumConstraint(
                "customer_orders",
                "status",
                "customer_orders_status_check",
                List.of(
                        "PENDING",
                        "PARTIALLY_FULFILLED",
                        "FULFILLED",
                        "CANCELLED"
                )
        );

        LOGGER.info("Verified customer_orders_status_check constraint");
    }

    private void repairEnumConstraint(
            String tableName,
            String columnName,
            String constraintName,
            List<String> allowedValues
    ) {

        if (!tableExists(tableName)) {
            return;
        }

        dropTableCheckConstraintsContaining(
                tableName,
                columnName
        );

        String quotedValues =
                allowedValues.stream()
                        .map(value -> "'" + value.replace("'", "''") + "'")
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("''");

        jdbcTemplate.execute(
                "ALTER TABLE " + quoteIdentifier(tableName) +
                        " ADD CONSTRAINT " + quoteIdentifier(constraintName) +
                        " CHECK (" + columnName + " IS NULL OR " + columnName +
                        " IN (" + quotedValues + "))"
        );
    }

    private void dropTableCheckConstraintsContaining(
            String tableName,
            String columnName
    ) {

        if (!tableExists(tableName)) {
            return;
        }

        List<String> checkConstraints =
                jdbcTemplate.queryForList(
                        """
                        SELECT conname
                        FROM pg_constraint
                        WHERE conrelid = ('public.' || ?)::regclass
                        AND contype = 'c'
                        AND lower(pg_get_constraintdef(oid)) LIKE ?
                        """,
                        String.class,
                        tableName,
                        "%" + columnName.toLowerCase(Locale.ROOT) + "%"
                );

        for (String constraint : checkConstraints) {
            jdbcTemplate.execute(
                    "ALTER TABLE " + quoteIdentifier(tableName) +
                            " DROP CONSTRAINT IF EXISTS " +
                            quoteIdentifier(constraint)
            );
        }
    }

    private void dropCustomerUniqueConstraintsContaining(
            String columnName
    ) {

        List<String> uniqueConstraints =
                jdbcTemplate.queryForList(
                        """
                        SELECT conname
                        FROM pg_constraint
                        WHERE conrelid = 'public.customers'::regclass
                        AND contype = 'u'
                        AND lower(pg_get_constraintdef(oid)) LIKE ?
                        """,
                        String.class,
                        "%" + columnName.toLowerCase(Locale.ROOT) + "%"
                );

        for (String constraint : uniqueConstraints) {
            jdbcTemplate.execute(
                    "ALTER TABLE customers DROP CONSTRAINT IF EXISTS " +
                            quoteIdentifier(constraint)
            );
        }
    }

    private void dropCustomerColumnNotNull(
            String columnName
    ) {

        Boolean columnExists =
                jdbcTemplate.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.columns
                            WHERE table_schema = 'public'
                            AND table_name = 'customers'
                            AND column_name = ?
                        )
                        """,
                        Boolean.class,
                        columnName
                );

        if (!Boolean.TRUE.equals(columnExists)) {
            return;
        }

        jdbcTemplate.execute(
                "ALTER TABLE customers ALTER COLUMN " +
                        quoteIdentifier(columnName) +
                " DROP NOT NULL"
        );
    }

    private void dropCustomerUniqueIndexesContaining(
            String columnName
    ) {

        List<String> uniqueIndexes =
                jdbcTemplate.queryForList(
                        """
                        SELECT index_class.relname
                        FROM pg_index index_info
                        JOIN pg_class index_class
                          ON index_class.oid = index_info.indexrelid
                        JOIN pg_class table_class
                          ON table_class.oid = index_info.indrelid
                        JOIN pg_namespace namespace
                          ON namespace.oid = table_class.relnamespace
                        WHERE namespace.nspname = 'public'
                        AND table_class.relname = 'customers'
                        AND index_info.indisunique = true
                        AND index_info.indisprimary = false
                        AND lower(pg_get_indexdef(index_info.indexrelid)) LIKE ?
                        """,
                        String.class,
                        "%" + columnName.toLowerCase(Locale.ROOT) + "%"
                );

        for (String indexName : uniqueIndexes) {
            jdbcTemplate.execute(
                    "DROP INDEX IF EXISTS " + quoteIdentifier(indexName)
            );
        }
    }

    private boolean tableExists(
            String tableName
    ) {

        Boolean exists =
                jdbcTemplate.queryForObject(
                        "SELECT to_regclass(?) IS NOT NULL",
                        Boolean.class,
                        "public." + tableName
                );

        return Boolean.TRUE.equals(exists);
    }

    private String quoteIdentifier(
            String value
    ) {

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
