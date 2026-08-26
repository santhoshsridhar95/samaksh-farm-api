package com.samaksh.farms.audit.service;

import com.samaksh.farms.audit.dto.AuditResponse;
import com.samaksh.farms.audit.entity.AuditLog;
import com.samaksh.farms.audit.repo.AuditLogRepository;
import com.samaksh.farms.cashhandover.entity.CashHandover;
import com.samaksh.farms.cashhandover.repo.CashHandoverRepository;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.exchangebox.entity.ExchangeBoxHandover;
import com.samaksh.farms.exchangebox.repo.ExchangeBoxHandoverRepository;
import com.samaksh.farms.harvest.entity.Harvest;
import com.samaksh.farms.harvest.repo.HarvestRepository;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import com.samaksh.farms.order.entity.CustomerOrder;
import com.samaksh.farms.order.repo.CustomerOrderRepository;
import com.samaksh.farms.products.entity.Product;
import com.samaksh.farms.products.repo.ProductRepository;
import com.samaksh.farms.review.entity.CustomerReview;
import com.samaksh.farms.review.repo.CustomerReviewRepository;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    private final UserRepository userRepository;

    private final CustomerRepository customerRepository;

    private final ProductRepository productRepository;

    private final SaleRepository saleRepository;

    private final CustomerOrderRepository orderRepository;

    private final CustomerReviewRepository reviewRepository;

    private final CashHandoverRepository cashHandoverRepository;

    private final ExchangeBoxHandoverRepository exchangeBoxHandoverRepository;

    private final InventoryTransactionRepository inventoryTransactionRepository;

    private final HarvestRepository harvestRepository;

    public void createAudit(
            Authentication authentication,
            String module,
            String action,
            String referenceId,
            String remarks
    ) {

        Long userId = 0L;

        String userName = "SYSTEM";

        String userEmail = "SYSTEM";

        if (authentication != null
                && authentication.getPrincipal()
                instanceof User user) {

            userId = user.getId();

            userName = user.getName();

            userEmail = user.getEmail();
        }

        AuditLog auditLog =
                AuditLog.builder()
                        .userId(
                                userId
                        )
                        .userName(
                                userName
                        )
                        .userEmail(
                                userEmail
                        )
                        .module(
                                module
                        )
                        .action(
                                action
                        )
                        .referenceId(
                                referenceId
                        )
                        .remarks(
                                remarks
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        auditLogRepository.save(
                auditLog
        );
    }

    public List<AuditResponse> getAuditLogs() {

        return auditLogRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AuditResponse> getAuditLogsNewestFirst() {

        return auditLogRepository.findAll(
                        Sort.by("createdAt")
                                .descending()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditResponse mapToResponse(
            AuditLog auditLog
    ) {
        String referenceName =
                resolveReferenceName(auditLog);

        return AuditResponse.builder()
                .id(
                        auditLog.getId()
                )
                .userId(
                        auditLog.getUserId()
                )
                .userName(
                        auditLog.getUserName()
                )
                .userEmail(
                        auditLog.getUserEmail()
                )
                .module(
                        auditLog.getModule()
                )
                .action(
                        auditLog.getAction()
                )
                .referenceId(
                        auditLog.getReferenceId()
                )
                .referenceName(
                        referenceName
                )
                .remarks(
                        readableRemarks(
                                auditLog,
                                referenceName
                        )
                )
                .createdAt(
                        auditLog.getCreatedAt()
                )
                .build();
    }

    private String resolveReferenceName(
            AuditLog auditLog
    ) {
        Long referenceId =
                parseReferenceId(auditLog.getReferenceId());

        if (referenceId == null) {
            if ("USER".equalsIgnoreCase(String.valueOf(auditLog.getModule()))) {
                String reference =
                        normalizeReference(auditLog.getReferenceId());

                if (reference == null) {
                    return null;
                }

                return userRepository.findByEmail(reference)
                        .map(this::userLabel)
                        .orElseGet(
                                () -> reference
                        );
            }

            return normalizeReference(auditLog.getReferenceId());
        }

        try {
            return switch (String.valueOf(auditLog.getModule()).toUpperCase()) {
                case "USER" -> userRepository.findById(referenceId)
                        .map(this::userLabel)
                        .orElse(null);
                case "CUSTOMER" -> customerRepository.findById(referenceId)
                        .map(this::customerLabel)
                        .orElse(null);
                case "PRODUCT" -> productRepository.findById(referenceId)
                        .map(this::productLabel)
                        .orElse(null);
                case "SALE" -> saleRepository.findById(referenceId)
                        .map(this::saleLabel)
                        .orElse(null);
                case "ORDER" -> orderRepository.findById(referenceId)
                        .map(this::orderLabel)
                        .orElse(null);
                case "REVIEW" -> reviewRepository.findById(referenceId)
                        .map(this::reviewLabel)
                        .orElse(null);
                case "CASH_HANDOVER" -> cashHandoverRepository.findById(referenceId)
                        .map(this::cashHandoverLabel)
                        .orElse(null);
                case "EXCHANGE_BOX" -> exchangeBoxHandoverRepository.findById(referenceId)
                        .map(this::exchangeBoxHandoverLabel)
                        .orElse(null);
                case "INVENTORY" -> inventoryTransactionRepository.findById(referenceId)
                        .map(this::inventoryTransactionLabel)
                        .orElse(null);
                case "HARVEST" -> harvestRepository.findById(referenceId)
                        .map(this::harvestLabel)
                        .orElse(null);
                default -> null;
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseReferenceId(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String userLabel(
            User user
    ) {
        return firstPresent(
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber()
        );
    }

    private String customerLabel(
            Customer customer
    ) {
        return joinLabel(
                firstPresent(
                        customer.getCustomerName(),
                        customer.getContactPerson()
                ),
                customer.getLocation()
        );
    }

    private String productLabel(
            Product product
    ) {
        return firstPresent(
                product.getProductName(),
                product.getProductCode()
        );
    }

    private String saleLabel(
            Sale sale
    ) {
        String customerName =
                sale.getCustomer() == null
                        ? null
                        : sale.getCustomer().getCustomerName();

        String productName =
                sale.getProduct() == null
                        ? null
                        : sale.getProduct().getProductName();

        return joinLabel(
                customerName,
                productName
        );
    }

    private String orderLabel(
            CustomerOrder order
    ) {
        String customerName =
                order.getCustomer() == null
                        ? null
                        : order.getCustomer().getCustomerName();

        return joinLabel(
                firstPresent(order.getOrderCode(), customerName),
                customerName
        );
    }

    private String reviewLabel(
            CustomerReview review
    ) {
        return joinLabel(
                review.getName(),
                review.getLocation()
        );
    }

    private String cashHandoverLabel(
            CashHandover handover
    ) {
        return joinLabel(
                handover.getCollectorName(),
                handover.getOwnerName()
        );
    }

    private String exchangeBoxHandoverLabel(
            ExchangeBoxHandover handover
    ) {
        return joinLabel(
                handover.getCollectorName(),
                handover.getOwnerName()
        );
    }

    private String inventoryTransactionLabel(
            InventoryTransaction transaction
    ) {
        return joinLabel(
                String.valueOf(transaction.getInventoryType()),
                String.valueOf(transaction.getTransactionType())
        );
    }

    private String harvestLabel(
            Harvest harvest
    ) {
        return joinLabel(
                harvest.getBatch() == null
                        ? null
                        : harvest.getBatch().getBatchCode(),
                harvest.getHarvestDate() == null
                        ? null
                        : harvest.getHarvestDate().toString()
        );
    }

    private String joinLabel(
            String primary,
            String secondary
    ) {
        String safePrimary =
                normalizeReference(primary);

        String safeSecondary =
                normalizeReference(secondary);

        if (safePrimary == null) {
            return safeSecondary;
        }

        if (safeSecondary == null || safePrimary.equals(safeSecondary)) {
            return safePrimary;
        }

        return safePrimary + " - " + safeSecondary;
    }

    private String firstPresent(
            String... values
    ) {
        for (String value : values) {
            String normalized =
                    normalizeReference(value);

            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

    private String normalizeReference(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String readableRemarks(
            AuditLog auditLog,
            String referenceName
    ) {
        String action =
                String.valueOf(auditLog.getAction()).toUpperCase();

        String module =
                String.valueOf(auditLog.getModule()).toUpperCase();

        String base =
                switch (action) {
                    case "CREATE_USER" -> "Created user";
                    case "APPROVE_USER" -> "Approved user";
                    case "REJECT_USER" -> "Rejected user";
                    case "ENABLE_USER" -> "Enabled user login";
                    case "DISABLE_USER" -> "Disabled user login";
                    case "DELETE_USER" -> "Soft deleted user";
                    case "RESET_PASSWORD" -> "Reset user password";
                    case "CHANGE_ROLE" -> "Changed access roles";
                    case "CHANGE_ENTITLEMENTS" -> "Changed extra access permissions";
                    case "CREATE_CUSTOMER" -> "Created shop";
                    case "UPDATE_CUSTOMER" -> "Updated shop details";
                    case "DELETE_CUSTOMER" -> "Deleted shop";
                    case "CREATE_PRODUCT" -> "Created product";
                    case "UPDATE_PRODUCT" -> "Updated product";
                    case "ENABLE_PRODUCT" -> "Enabled product";
                    case "DISABLE_PRODUCT" -> "Disabled product";
                    case "CREATE_SALE" -> "Created delivery entry";
                    case "UPDATE_SALE_ENTRY" -> "Corrected delivery entry";
                    case "UPDATE_PAYMENT" -> "Updated payment";
                    case "APPLY_DELIVERY_COLLECTION" ->
                            "Applied received cash to an older pending sale";
                    case "CREATE_CASH_HANDOVER" -> "Recorded cash handover";
                    case "UPDATE_CASH_HANDOVER" -> "Updated cash handover";
                    case "RECEIVE_EXCHANGE_BOXES" -> "Recorded exchange boxes returned";
                    case "UPDATE_REVIEW" -> "Updated customer review";
                    case "DELETE_REVIEW" -> "Deleted customer review";
                    default -> formatAction(action) + " in " + formatAction(module);
                };

        String target =
                normalizeReference(referenceName);

        String sentence =
                target == null
                        ? base
                        : base + " for " + target;

        String details =
                cleanRemarks(auditLog.getRemarks());

        if (details == null || isGenericRemark(details)) {
            return sentence;
        }

        if (details.toLowerCase().contains(sentence.toLowerCase())) {
            return details;
        }

        return sentence + " - " + details;
    }

    private String cleanRemarks(
            String remarks
    ) {
        String value =
                normalizeReference(remarks);

        if (value == null) {
            return null;
        }

        return value
                .replace("Sale Amount : ", "Bill Rs. ")
                .replace("Sale Amount: ", "Bill Rs. ")
                .replace(", received today Rs. ", ", received Rs. ")
                .replace(", applied to this sale Rs. ", ", applied here Rs. ");
    }

    private boolean isGenericRemark(
            String remarks
    ) {
        String value =
                remarks.toLowerCase();

        return value.equals("user created")
                || value.equals("user approved")
                || value.equals("user rejected")
                || value.equals("user disabled")
                || value.equals("user enabled")
                || value.equals("user soft deleted")
                || value.equals("password reset")
                || value.equals("extra entitlements changed")
                || value.equals("customer created")
                || value.equals("customer updated")
                || value.equals("product created")
                || value.equals("product updated")
                || value.equals("product disabled")
                || value.equals("product enabled")
                || value.equals("customer review updated")
                || value.equals("customer review deleted");
    }

    private String formatAction(
            String value
    ) {
        String[] parts =
                value.toLowerCase().split("_");

        StringBuilder label =
                new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!label.isEmpty()) {
                label.append(" ");
            }

            label.append(
                    Character.toUpperCase(part.charAt(0))
            ).append(
                    part.substring(1)
            );
        }

        return label.toString();
    }
}
