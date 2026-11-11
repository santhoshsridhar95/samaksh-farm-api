package com.samaksh.farms.sale.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.customer.repo.CustomerRepository;
import com.samaksh.farms.enums.ExchangeType;
import com.samaksh.farms.enums.PaymentStatus;
import com.samaksh.farms.enums.ProductUnitType;
import com.samaksh.farms.products.entity.Product;
import com.samaksh.farms.products.repo.ProductRepository;
import com.samaksh.farms.sale.dto.PagedResponse;
import com.samaksh.farms.sale.dto.SalePaymentUpdateRequest;
import com.samaksh.farms.sale.dto.SaleRequest;
import com.samaksh.farms.sale.dto.SaleResponse;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class SaleService {

    private final com.samaksh.farms.finance.MoneyService moneyService;

    private final SaleRepository saleRepository;

    private final CustomerRepository customerRepository;

    private final ProductRepository productRepository;

    private final AuditService auditService;

    private final UserRepository userRepository;

    public SaleResponse createSale(
            SaleRequest request,
            Authentication authentication
    ) {
        moneyService.prepare();


        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException(
                    "Shop is required"
            );
        }

        Customer customer =
                customerRepository.findById(
                        request.getCustomerId()
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Customer",
                                request.getCustomerId()
                        )
                );

        double quantity =
                request.getQuantity() == null
                        ? 0
                        : request.getQuantity();

        double unitPrice =
                request.getUnitPrice() == null
                        ? 0
                        : request.getUnitPrice();

        if (!Double.isFinite(quantity) || quantity < 0 || (request.isDirectSale() && quantity == 0)) {
            throw new IllegalArgumentException("Boxes cannot be negative; direct sales require a positive quantity");
        }
        boolean collectionOnly = quantity == 0;

        if (unitPrice < 0) {
            throw new IllegalArgumentException(
                    "Unit price cannot be negative"
            );
        }

        Product product = collectionOnly ? saleRepository.findByCustomerIdAndPaymentStatusInOrderBySaleDateAsc(customer.getId(), List.of(PaymentStatus.PENDING, PaymentStatus.PARTIAL)).stream().map(Sale::getProduct).filter(java.util.Objects::nonNull).findFirst().orElse(null) :
                resolveProduct(
                        request,
                        unitPrice
                );

        double totalAmount =
                quantity * unitPrice;

        ExchangeType exchangeType =
                request.getExchangeType() == null
                        ? ExchangeType.NONE
                        : request.getExchangeType();

        double exchangeBoxes =
                request.getExchangeBoxes() == null
                        ? 0
                        : request.getExchangeBoxes();

        if (exchangeBoxes < 0) {
            throw new IllegalArgumentException(
                    "Exchange boxes cannot be negative"
            );
        }

        double returnedBoxes =
                request.getReturnedBoxes() == null
                        ? 0
                        : request.getReturnedBoxes();

        if (returnedBoxes < 0) {
            throw new IllegalArgumentException(
                    "Returned boxes cannot be negative"
            );
        }

        Double shopkeeperSellingPrice =
                request.getShopkeeperSellingPrice();

        if (shopkeeperSellingPrice != null &&
                shopkeeperSellingPrice < 0) {
            throw new IllegalArgumentException(
                    "Shopkeeper selling price cannot be negative"
            );
        }

        double exchangeCredit =
                exchangeCredit(
                        exchangeType,
                        exchangeBoxes,
                        unitPrice
                );

        double billableAmount =
                Math.max(
                        0,
                        totalAmount - exchangeCredit
                );

        double receivedToday =
                request.getAmountCollected() == null
                        ? 0
                        : request.getAmountCollected();

        if (!Double.isFinite(receivedToday) || receivedToday < 0) {
            throw new IllegalArgumentException(
                    "Collected amount cannot be negative"
            );
        }

        if (collectionOnly && receivedToday <= 0) throw new IllegalArgumentException("Enter an amount received to record a zero-box collection");
        if (collectionOnly && (exchangeBoxes > 0 || returnedBoxes > 0)) throw new IllegalArgumentException("Record box returns separately from an amount-only collection");

        double outstandingBeforeSale =
                customerPendingBalance(
                        customer.getId()
                );

        if (receivedToday > outstandingBeforeSale + billableAmount) {
            throw new IllegalArgumentException(
                    "Collected amount cannot be greater than shop outstanding plus today's bill"
            );
        }

        CashCollector cashCollector =
                resolveCashCollector(
                        request,
                        authentication
                );

        double remainingCollection = request.isDirectSale() ? receivedToday :
                applyCollectionToPreviousSales(
                        customer.getId(),
                        receivedToday,
                        authentication,
                        cashCollector
                );

        double amountAppliedToCurrentSale =
                Math.min(
                        billableAmount,
                        remainingCollection
                );

        double pendingAmount =
                billableAmount - amountAppliedToCurrentSale;

        PaymentStatus paymentStatus = collectionOnly ? PaymentStatus.PAID :
                resolvePaymentStatus(
                        billableAmount,
                        amountAppliedToCurrentSale
                );

        LocalDateTime saleDate =
                resolveSaleDate(
                        request,
                        authentication
                );

        Sale sale =
                Sale.builder()
                        .salesChannel(collectionOnly ? "COLLECTION" : request.isDirectSale() ? "DIRECT" : "SHOP")
                        .customer(customer)
                        .product(product)
                        .quantity(
                                quantity
                        )
                        .unitPrice(
                                unitPrice
                        )
                        .totalAmount(
                                billableAmount
                        )
                        .amountCollected(
                                amountAppliedToCurrentSale
                        )
                        .amountReceivedToday(
                                receivedToday
                        )
                        .pendingAmount(
                                pendingAmount
                        )
                        .shopkeeperSellingPrice(
                                shopkeeperSellingPrice
                        )
                        .exchangeType(
                                exchangeType
                        )
                        .exchangeBoxes(
                                exchangeBoxes
                        )
                        .returnedBoxes(
                                returnedBoxes
                        )
                        .paymentStatus(
                                paymentStatus
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .createdByUserId(
                                currentUserId(
                                        authentication
                                )
                        )
                        .createdByName(
                                currentUserName(
                                        authentication
                                )
                        )
                        .createdByEmail(
                                currentUserEmail(
                                        authentication
                                )
                        )
                        .collectorUserId(
                                cashCollector.userId()
                        )
                        .collectorName(
                                cashCollector.name()
                        )
                        .collectorEmail(
                                cashCollector.email()
                        )
                        .saleDate(
                                saleDate
                        )
                        .build();

        Sale savedSale =
                saleRepository.save(
                        sale
                );

        moneyService.collection(cashCollector.userId(), cashCollector.name(), cashCollector.email(), receivedToday, savedSale.getId(), authentication);

        auditService.createAudit(
                authentication,
                "SALE",
                collectionOnly ? "RECORD_SHOP_COLLECTION" : "CREATE_SALE",
                savedSale.getId().toString(),
                "Sale Amount : "
                        + billableAmount
                        + ", received today Rs. "
                        + receivedToday
                        + ", applied to this sale Rs. "
                        + amountAppliedToCurrentSale
        );

        return mapToResponse(
                savedSale
        );
    }

    public PagedResponse<SaleResponse> getSales(
            int page,
            int size,
            Long customerId,
            PaymentStatus paymentStatus
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("saleDate")
                                .descending()
                );

        Page<Sale> sales;

        if (customerId != null &&
                paymentStatus != null) {

            sales =
                    saleRepository
                            .findByCustomerIdAndPaymentStatus(
                                    customerId,
                                    paymentStatus,
                                    pageable
                            );

        } else if (customerId != null) {

            sales =
                    saleRepository.findByCustomerId(
                            customerId,
                            pageable
                    );

        } else if (paymentStatus != null) {

            sales =
                    saleRepository.findByPaymentStatus(
                            paymentStatus,
                            pageable
                    );

        } else {

            sales =
                    saleRepository.findAll(
                            pageable
                    );
        }

        return PagedResponse
                .<SaleResponse>builder()
                .content(
                        sales.getContent()
                                .stream()
                                .map(this::mapToResponse)
                                .toList()
                )
                .page(
                        sales.getNumber()
                )
                .size(
                        sales.getSize()
                )
                .totalElements(
                        sales.getTotalElements()
                )
                .totalPages(
                        sales.getTotalPages()
                )
                .last(
                        sales.isLast()
                )
                .build();
    }

    public SaleResponse updatePayment(
            Long saleId,
            SalePaymentUpdateRequest request,
            Authentication authentication
    ) {
        moneyService.prepare();


        Sale sale =
                saleRepository.findById(
                        saleId
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Sale",
                                saleId
                        )
                );

        double totalAmount =
                saleTotal(
                        sale
                );

        double newAmountCollected =
                request.getAmountCollected() == null
                        ? saleCollected(
                                sale
                        )
                        : request.getAmountCollected();

        if (newAmountCollected < 0) {
            throw new IllegalArgumentException(
                    "Collected amount cannot be negative"
            );
        }

        if (newAmountCollected > totalAmount) {
            throw new IllegalArgumentException(
                    "Collected amount cannot be greater than total amount"
            );
        }

        double oldAmountCollected =
                saleCollected(
                        sale
                );

        double oldPendingAmount =
                salePending(
                        sale
                );

        PaymentStatus oldPaymentStatus =
                sale.getPaymentStatus();

        double newPendingAmount =
                Math.max(
                        0,
                        totalAmount - newAmountCollected
                );

        PaymentStatus newPaymentStatus =
                resolvePaymentStatus(
                        totalAmount,
                        newAmountCollected
                );

        moneyService.collection(currentUserId(authentication), currentUserName(authentication), currentUserEmail(authentication), newAmountCollected - oldAmountCollected, sale.getId(), authentication);

        sale.setAmountCollected(
                newAmountCollected
        );
        sale.setPendingAmount(
                newPendingAmount
        );
        sale.setPaymentStatus(
                newPaymentStatus
        );
        applyUpdatedBy(
                sale,
                authentication
        );

        if (request.getRemarks() != null &&
                !request.getRemarks().isBlank()) {

            String existingRemarks =
                    sale.getRemarks() == null
                            ? ""
                            : sale.getRemarks();

            sale.setRemarks(
                    existingRemarks.isBlank()
                            ? request.getRemarks()
                            : existingRemarks + " | Payment: "
                                    + request.getRemarks()
            );
        }

        Sale savedSale =
                saleRepository.save(
                        sale
                );

        auditService.createAudit(
                authentication,
                "SALE",
                "UPDATE_PAYMENT",
                savedSale.getId().toString(),
                "Payment changed for "
                        + savedSale.getCustomer().getCustomerName()
                        + ": collected Rs. "
                        + oldAmountCollected
                        + " -> Rs. "
                        + newAmountCollected
                        + ", pending Rs. "
                        + oldPendingAmount
                        + " -> Rs. "
                        + newPendingAmount
                        + ", status "
                        + oldPaymentStatus
                        + " -> "
                        + newPaymentStatus
        );

        return mapToResponse(
                savedSale
        );
    }

    public void correctDirectSale(Long id, SaleRequest request, String location, Authentication auth) {
        moneyService.prepare();
        if(!isSuperAdmin(currentUser(auth)))throw new org.springframework.security.access.AccessDeniedException("Only super admin can edit purchases");
        var sale=saleRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Sale not found"));
        if(!"DIRECT".equals(sale.getSalesChannel()))throw new IllegalArgumentException("Choose a direct sale");
        var customer=customerRepository.findById(request.getCustomerId()).orElseThrow(()->new IllegalArgumentException("Customer not found"));
        if(!"DIRECT".equals(customer.getCustomerType()))throw new IllegalArgumentException("Choose a direct customer");
        var collector=userRepository.findById(request.getCollectorUserId()).orElseThrow(()->new IllegalArgumentException("Collector not found"));
        if(!Boolean.TRUE.equals(collector.getActive())&&!java.util.Objects.equals(collector.getId(),sale.getCollectorUserId()))throw new IllegalArgumentException("Choose an active collector");
        var total=java.math.BigDecimal.valueOf(request.getQuantity()).multiply(java.math.BigDecimal.valueOf(request.getUnitPrice())).setScale(2,java.math.RoundingMode.HALF_UP);
        if(request.getQuantity()<=0 || request.getUnitPrice()<=0 || request.getAmountCollected()<0 || java.math.BigDecimal.valueOf(request.getAmountCollected()).compareTo(total)>0)throw new IllegalArgumentException("Invalid quantity, price or collection");
        if(request.getDeliveryDate()==null||request.getDeliveryDate().isAfter(BusinessTime.today()))throw new IllegalArgumentException("Choose a purchase date no later than today");
        if(!java.util.Objects.equals(sale.getCollectorUserId(),collector.getId()) || Math.round(saleCollected(sale)*100)!=Math.round(request.getAmountCollected()*100))
        moneyService.correctDirectCollection(id,collector.getId(),collector.getName(),collector.getEmail(),request.getAmountCollected(),request.getDeliveryDate(),auth);
        sale.setCustomer(customer);sale.setProduct(resolveProduct(request,request.getUnitPrice()));sale.setQuantity(request.getQuantity());sale.setUnitPrice(request.getUnitPrice());
        sale.setTotalAmount(total.doubleValue());sale.setAmountCollected(request.getAmountCollected());sale.setAmountReceivedToday(request.getAmountCollected());sale.setPendingAmount(total.subtract(java.math.BigDecimal.valueOf(request.getAmountCollected())).doubleValue());
        sale.setPaymentStatus(resolvePaymentStatus(total.doubleValue(),request.getAmountCollected()));
        sale.setCollectorUserId(collector.getId());sale.setCollectorName(collector.getName());sale.setCollectorEmail(collector.getEmail());
        sale.setSaleDate(request.getDeliveryDate().atTime(sale.getSaleDate()==null?BusinessTime.now().toLocalTime():sale.getSaleDate().toLocalTime()));
        sale.setPurchaseLocation(location.trim());sale.setRemarks(request.getRemarks());applyUpdatedBy(sale,auth);saleRepository.save(sale);
    }

    public SaleResponse updateSaleEntry(
            Long saleId,
            SaleRequest request,
            Authentication authentication
    ) {
        moneyService.prepare();


        User currentUser =
                currentUser(authentication);

        if (!isSuperAdmin(currentUser)) {
            throw new IllegalArgumentException(
                    "Only super admin can edit delivery entries"
            );
        }

        Sale sale =
                saleRepository.findById(
                        saleId
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Sale",
                                saleId
                        )
                );

        Customer customer =
                request.getCustomerId() == null
                        ? sale.getCustomer()
                        : customerRepository.findById(
                                request.getCustomerId()
                        ).orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Customer",
                                        request.getCustomerId()
                                )
                        );

        double quantity =
                request.getQuantity() == null
                        ? saleQuantity(sale)
                        : request.getQuantity();

        double unitPrice =
                request.getUnitPrice() == null
                        ? saleUnitPrice(sale)
                        : request.getUnitPrice();

        boolean collectionOnly = quantity == 0;
        if (!Double.isFinite(quantity) || quantity < 0)throw new IllegalArgumentException("Boxes cannot be negative");

        if (!Double.isFinite(unitPrice) || unitPrice < 0) {
            throw new IllegalArgumentException(
                    "Unit price cannot be negative"
            );
        }

        ExchangeType exchangeType =
                request.getExchangeType() == null
                        ? sale.getExchangeType()
                        : request.getExchangeType();

        if (exchangeType == null) {
            exchangeType = ExchangeType.NONE;
        }

        double exchangeBoxes =
                request.getExchangeBoxes() == null
                        ? safeDouble(sale.getExchangeBoxes())
                        : request.getExchangeBoxes();

        if (exchangeBoxes < 0) {
            throw new IllegalArgumentException(
                    "Exchange boxes cannot be negative"
            );
        }

        double returnedBoxes =
                request.getReturnedBoxes() == null
                        ? safeDouble(sale.getReturnedBoxes())
                        : request.getReturnedBoxes();

        if (returnedBoxes < 0) {
            throw new IllegalArgumentException(
                    "Returned boxes cannot be negative"
            );
        }

        Double shopkeeperSellingPrice =
                request.getShopkeeperSellingPrice() == null
                        ? sale.getShopkeeperSellingPrice()
                        : request.getShopkeeperSellingPrice();

        if (shopkeeperSellingPrice != null &&
                shopkeeperSellingPrice < 0) {
            throw new IllegalArgumentException(
                    "Shopkeeper selling price cannot be negative"
            );
        }


        String beforeCorrection = "date="+sale.getSaleDate()+", quantity="+sale.getQuantity()+", price="+sale.getUnitPrice()+", received="+saleAmountReceivedToday(sale)+", shop="+sale.getCustomer().getId()+", remarks="+sale.getRemarks();
        Product product = collectionOnly && "Balance collection".equals(request.getProductName()) ? sale.getProduct() :
                resolveProduct(
                        request,
                        unitPrice
                );

        double billableAmount =
                Math.max(
                        0,
                        quantity * unitPrice - exchangeCredit(
                                exchangeType,
                                exchangeBoxes,
                                unitPrice
                        )
                );

        double amountReceivedToday =
                request.getAmountReceivedToday() == null
                        ? request.getAmountCollected() == null
                                ? saleAmountReceivedToday(sale)
                                : request.getAmountCollected()
                        : request.getAmountReceivedToday();

        if (!Double.isFinite(amountReceivedToday) || amountReceivedToday < 0) {
            throw new IllegalArgumentException(
                    "Received amount cannot be negative"
            );
        }

        double previousReceived=saleAmountReceivedToday(sale);
        Long oldCustomerId=sale.getCustomer().getId();
        boolean moved=!java.util.Objects.equals(oldCustomerId,customer.getId());
        var oldSales=new java.util.ArrayList<>(saleRepository.findByCustomerIdOrderBySaleDateAscIdAsc(oldCustomerId));
        if(oldSales.stream().noneMatch(v->v.getId().equals(saleId)))oldSales.add(sale);
        var newSales=moved?new java.util.ArrayList<>(saleRepository.findByCustomerIdOrderBySaleDateAscIdAsc(customer.getId())):oldSales;
        long oldCash=oldSales.stream().mapToLong(v->Math.round(saleCollected(v)*100)).sum();
        long newCash=moved?newSales.stream().mapToLong(v->Math.round(saleCollected(v)*100)).sum():oldCash;
        if(moved){oldSales.removeIf(v->v.getId().equals(saleId));newSales.add(sale);oldCash-=Math.round(previousReceived*100);newCash+=Math.round(amountReceivedToday*100);}
        else newCash+=Math.round((amountReceivedToday-previousReceived)*100);
        double amountCollected=0, pendingAmount=billableAmount;

        sale.setCustomer(customer);
        sale.setProduct(product);
        sale.setQuantity(quantity);
        sale.setSalesChannel(collectionOnly ? "COLLECTION" : "DIRECT".equals(sale.getSalesChannel())?"DIRECT":"SHOP");
        sale.setUnitPrice(unitPrice);
        sale.setTotalAmount(billableAmount);
        sale.setAmountCollected(amountCollected);
        sale.setAmountReceivedToday(amountReceivedToday);
        sale.setPendingAmount(pendingAmount);
        sale.setShopkeeperSellingPrice(shopkeeperSellingPrice);
        sale.setExchangeType(exchangeType);
        sale.setExchangeBoxes(exchangeBoxes);
        sale.setReturnedBoxes(returnedBoxes);
        sale.setPaymentStatus(
                collectionOnly ? PaymentStatus.PAID : resolvePaymentStatus(
                        billableAmount,
                        amountCollected
                )
        );

        if (request.getDeliveryDate() != null) {
            sale.setSaleDate(
                    LocalDateTime.of(
                            request.getDeliveryDate(),
                            sale.getSaleDate() == null
                                    ? BusinessTime.now().toLocalTime()
                                    : sale.getSaleDate().toLocalTime()
                    )
            );
        }

        sale.setRemarks(request.getRemarks());

        applyUpdatedBy(
                sale,
                authentication
        );

        if(moved)reallocateCorrectedShop(oldSales,oldCash,authentication);
        reallocateCorrectedShop(newSales,newCash,authentication);
        moneyService.correctCollection(saleId,sale.getCreatedByUserId(),sale.getCreatedByName(),sale.getCreatedByEmail(),amountReceivedToday-previousReceived,sale.getSaleDate()==null?BusinessTime.today():sale.getSaleDate().toLocalDate(),authentication);
        amountCollected=saleCollected(sale);

        Sale savedSale =
                saleRepository.save(
                        sale
                );

        auditService.createAudit(
                authentication,
                "SALE",
                "UPDATE_SALE_ENTRY",
                savedSale.getId().toString(),
                "Before: "+beforeCorrection+" | After: date="+savedSale.getSaleDate()+", remarks="+savedSale.getRemarks()+"; Delivery entry corrected for "
                        + savedSale.getCustomer().getCustomerName()
                        + ": boxes "
                        + quantity
                        + ", bill Rs. "
                        + billableAmount
                        + ", collected Rs. "
                        + amountReceivedToday
                        + ", applied to this sale Rs. "
                        + amountCollected
        );

        return mapToResponse(
                savedSale
        );
    }

    private void reallocateCorrectedShop(List<Sale> rows,long receivedCents,Authentication auth) {
        long billed=rows.stream().mapToLong(v->Math.round(saleTotal(v)*100)).sum();
        if(receivedCents<0 || receivedCents>billed)throw new IllegalArgumentException("Corrected receipts must be between zero and the shop's total billed amount. Check the shop, boxes, price and amount received.");
        rows.sort(java.util.Comparator.comparing(Sale::getSaleDate,java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())).thenComparing(Sale::getId));
        long remaining=receivedCents;
        for(var row:rows){double before=saleCollected(row);long total=Math.round(saleTotal(row)*100);long applied=Math.min(total,remaining);remaining-=applied;
            row.setAmountCollected(applied/100d);row.setPendingAmount((total-applied)/100d);row.setPaymentStatus(total==0?PaymentStatus.PAID:resolvePaymentStatus(total/100d,applied/100d));
            if(Math.abs(before-applied/100d)>.001){applyUpdatedBy(row,auth);auditService.createAudit(auth,"SALE","REALLOCATE_CORRECTION",row.getId().toString(),"After correction, oldest invoices first: collected "+before+" -> "+row.getAmountCollected()+"; pending "+row.getPendingAmount());}
        }
        saleRepository.saveAll(rows);
    }

    private PaymentStatus resolvePaymentStatus(
            double totalAmount,
            double amountCollected
    ) {

        if (amountCollected >= totalAmount &&
                totalAmount > 0) {

            return PaymentStatus.PAID;
        }

        if (amountCollected > 0) {

            return PaymentStatus.PARTIAL;
        }

        return PaymentStatus.PENDING;
    }

    private double applyCollectionToPreviousSales(
            Long customerId,
            double collectionAmount,
            Authentication authentication,
            CashCollector cashCollector
    ) {

        if (collectionAmount <= 0) {
            return 0;
        }

        double remainingCollection =
                collectionAmount;

        List<Sale> unpaidSales =
                saleRepository.findByCustomerIdAndPaymentStatusInOrderBySaleDateAsc(
                        customerId,
                        List.of(
                                PaymentStatus.PENDING,
                                PaymentStatus.PARTIAL
                        )
                );

        for (Sale unpaidSale : unpaidSales) {

            if (remainingCollection <= 0) {
                break;
            }

            double pendingAmount =
                    salePending(
                            unpaidSale
                    );

            if (pendingAmount <= 0) {
                continue;
            }

            double appliedAmount =
                    Math.min(
                            pendingAmount,
                            remainingCollection
                    );

            double oldCollected =
                    saleCollected(
                            unpaidSale
                    );

            double oldPending =
                    pendingAmount;

            PaymentStatus oldStatus =
                    unpaidSale.getPaymentStatus();

            double newCollected =
                    oldCollected + appliedAmount;

            double newPending =
                    Math.max(
                            0,
                            saleTotal(
                                    unpaidSale
                            ) - newCollected
                    );

            unpaidSale.setAmountCollected(
                    newCollected
            );
            unpaidSale.setPendingAmount(
                    newPending
            );
            unpaidSale.setPaymentStatus(
                    resolvePaymentStatus(
                            saleTotal(
                                    unpaidSale
                            ),
                            newCollected
                    )
            );
            unpaidSale.setCollectorUserId(
                    cashCollector.userId()
            );
            unpaidSale.setCollectorName(
                    cashCollector.name()
            );
            unpaidSale.setCollectorEmail(
                    cashCollector.email()
            );
            applyUpdatedBy(
                    unpaidSale,
                    authentication
            );

            Sale savedSale =
                    saleRepository.save(
                            unpaidSale
                    );

            auditService.createAudit(
                    authentication,
                    "SALE",
                    "APPLY_DELIVERY_COLLECTION",
                    savedSale.getId().toString(),
                    "Delivery collection applied to previous sale: Rs. "
                            + appliedAmount
                            + ", collected Rs. "
                            + oldCollected
                            + " -> Rs. "
                            + newCollected
                            + ", pending Rs. "
                            + oldPending
                            + " -> Rs. "
                            + newPending
                            + ", status "
                            + oldStatus
                            + " -> "
                            + savedSale.getPaymentStatus()
            );

            remainingCollection -= appliedAmount;
        }

        return remainingCollection;
    }

    private double customerPendingBalance(
            Long customerId
    ) {

        return saleRepository.findByCustomerIdAndPaymentStatusInOrderBySaleDateAsc(
                        customerId,
                        List.of(
                                PaymentStatus.PENDING,
                                PaymentStatus.PARTIAL
                        )
                )
                .stream()
                .mapToDouble(this::salePending)
                .sum();
    }

    private double exchangeCredit(
            ExchangeType exchangeType,
            double exchangeBoxes,
            double unitPrice
    ) {

        if (exchangeBoxes <= 0 ||
                unitPrice <= 0) {

            return 0;
        }

        if (exchangeType == ExchangeType.ONE_ON_ONE) {

            return exchangeBoxes * unitPrice;
        }

        if (exchangeType == ExchangeType.TWO_ON_ONE) {

            return exchangeBoxes * unitPrice * 0.5;
        }

        return 0;
    }

    private double saleTotal(
            Sale sale
    ) {

        return sale.getTotalAmount() == null
                ? 0
                : sale.getTotalAmount();
    }

    private double saleCollected(
            Sale sale
    ) {

        return sale.getAmountCollected() == null
                ? 0
                : sale.getAmountCollected();
    }

    private double saleAmountReceivedToday(
            Sale sale
    ) {

        return sale.getAmountReceivedToday() == null
                ? saleCollected(sale)
                : sale.getAmountReceivedToday();
    }

    private double saleQuantity(
            Sale sale
    ) {

        return sale.getQuantity() == null
                ? 0
                : sale.getQuantity();
    }

    private double saleUnitPrice(
            Sale sale
    ) {

        return sale.getUnitPrice() == null
                ? 0
                : sale.getUnitPrice();
    }

    private double safeDouble(
            Double value
    ) {

        return value == null
                ? 0
                : value;
    }

    private double salePending(
            Sale sale
    ) {

        if (sale.getPendingAmount() != null) {

            return sale.getPendingAmount();
        }

        return saleTotal(sale) - saleCollected(sale);
    }

    private void applyUpdatedBy(
            Sale sale,
            Authentication authentication
    ) {

        sale.setUpdatedByUserId(
                currentUserId(
                        authentication
                )
        );
        sale.setUpdatedByName(
                currentUserName(
                        authentication
                )
        );
        sale.setUpdatedByEmail(
                currentUserEmail(
                        authentication
                )
        );
        sale.setUpdatedAt(
                BusinessTime.now()
        );
    }

    private CashCollector resolveCashCollector(
            SaleRequest request,
            Authentication authentication
    ) {

        User currentUser =
                currentUser(authentication);

        if (isSuperAdmin(currentUser) &&
                request.getCollectorUserId() != null) {

            User collector =
                    userRepository.findById(request.getCollectorUserId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "User",
                                            request.getCollectorUserId()
                                    )
                            );

            return new CashCollector(
                    collector.getId(),
                    collector.getName(),
                    collector.getEmail()
            );
        }

        if (isSuperAdmin(currentUser) &&
                request.getCollectorName() != null &&
                !request.getCollectorName().isBlank()) {

            return new CashCollector(
                    request.getCollectorUserId(),
                    request.getCollectorName().trim(),
                    optionalText(request.getCollectorEmail())
            );
        }

        return new CashCollector(
                currentUserId(authentication),
                currentUserName(authentication),
                currentUserEmail(authentication)
        );
    }

    private LocalDateTime resolveSaleDate(
            SaleRequest request,
            Authentication authentication
    ) {

        User currentUser =
                currentUser(authentication);

        if (!isSuperAdmin(currentUser) ||
                request.getDeliveryDate() == null) {
            return BusinessTime.now();
        }

        LocalDate deliveryDate =
                request.getDeliveryDate();

        return LocalDateTime.of(
                deliveryDate,
                BusinessTime.now()
                        .toLocalTime()
        );
    }

    private User currentUser(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {
            return user;
        }

        return null;
    }

    private boolean isSuperAdmin(
            User user
    ) {

        return user != null && (user.getRole() == com.samaksh.farms.enums.Role.SUPER_ADMIN ||
                (user.getExtraRoles() != null && user.getExtraRoles().contains(com.samaksh.farms.enums.Role.SUPER_ADMIN)));
    }

    private String optionalText(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private Long saleCollectorUserId(
            Sale sale
    ) {

        return sale.getCollectorUserId() == null
                ? sale.getCreatedByUserId()
                : sale.getCollectorUserId();
    }

    private String saleCollectorName(
            Sale sale
    ) {

        return sale.getCollectorName() == null ||
                sale.getCollectorName().isBlank()
                ? sale.getCreatedByName()
                : sale.getCollectorName();
    }

    private String saleCollectorEmail(
            Sale sale
    ) {

        return sale.getCollectorEmail() == null ||
                sale.getCollectorEmail().isBlank()
                ? sale.getCreatedByEmail()
                : sale.getCollectorEmail();
    }

    private Long currentUserId(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {

            return user.getId();
        }

        return 0L;
    }

    private String currentUserName(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {

            return user.getName();
        }

        return "SYSTEM";
    }

    private String currentUserEmail(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {

            return user.getEmail();
        }

        return "SYSTEM";
    }

    private record CashCollector(
            Long userId,
            String name,
            String email
    ) {}

    private Product resolveProduct(
            SaleRequest request,
            double unitPrice
    ) {

        if (request.getProductId() != null) {

            return productRepository.findById(
                    request.getProductId()
            ).orElseThrow(
                    () -> new ResourceNotFoundException(
                            "Product",
                            request.getProductId()
                    )
            );
        }

        String productName =
                request.getProductName() == null
                        ? ""
                        : request.getProductName().trim();

        if (productName.isBlank()) {
            throw new IllegalArgumentException(
                    "Product is required"
            );
        }

        return productRepository.findFirstByProductNameIgnoreCase(
                        productName
                )
                .map(product -> {
                    if (product.getActive() == null ||
                            !product.getActive()) {
                        product.setActive(true);
                    }

                    if (product.getStandardPrice() == null &&
                            unitPrice > 0) {
                        product.setStandardPrice(unitPrice);
                    }

                    return productRepository.save(product);
                })
                .orElseGet(() -> productRepository.save(
                        Product.builder()
                                .productCode(generateProductCode())
                                .productName(productName)
                                .unitType(ProductUnitType.BOX)
                                .standardPrice(unitPrice > 0
                                        ? unitPrice
                                        : 50)
                                .active(true)
                                .createdAt(BusinessTime.now())
                                .build()
                ));
    }

    private String generateProductCode() {

        long count =
                productRepository.count() + 1;

        return "SF-PROD-"
                + String.format(
                "%03d",
                count
        );
    }

    private SaleResponse mapToResponse(
            Sale sale
    ) {

        return SaleResponse.builder()
                .id(
                        sale.getId()
                )
                .customerId(
                        sale.getCustomer()
                                .getId()
                )
                .customerName(
                        sale.getCustomer()
                                .getCustomerName()
                )
                .shopCategory(
                        sale.getCustomer()
                                .getShopCategory()
                )
                .location(
                        sale.getCustomer()
                                .getLocation() == null
                                ? "R.T. Nagar"
                                : sale.getCustomer()
                                .getLocation()
                )
                .minimumBoxesPerDay(
                        sale.getCustomer()
                                .getMinimumBoxesPerDay()
                )
                .salesChannel(sale.getSalesChannel())
                .productId(sale.getProduct() == null ? null : sale.getProduct().getId())
                .productName(sale.getProduct() == null ? ("COLLECTION".equals(sale.getSalesChannel()) ? "Balance collection" : "Sale") : sale.getProduct().getProductName())
                .quantity(
                        sale.getQuantity()
                )
                .unitPrice(
                        sale.getUnitPrice()
                )
                .totalAmount(
                        sale.getTotalAmount()
                )
                .amountCollected(
                        sale.getAmountCollected()
                )
                .amountReceivedToday(
                        saleAmountReceivedToday(sale)
                )
                .pendingAmount(
                        sale.getPendingAmount()
                )
                .shopkeeperSellingPrice(
                        sale.getShopkeeperSellingPrice()
                )
                .exchangeType(
                        sale.getExchangeType()
                )
                .exchangeBoxes(
                        sale.getExchangeBoxes()
                )
                .returnedBoxes(
                        sale.getReturnedBoxes()
                )
                .paymentStatus(
                        sale.getPaymentStatus()
                )
                .remarks(
                        sale.getRemarks()
                )
                .createdByUserId(
                        sale.getCreatedByUserId()
                )
                .createdByName(
                        sale.getCreatedByName()
                )
                .createdByEmail(
                        sale.getCreatedByEmail()
                )
                .collectorUserId(
                        saleCollectorUserId(sale)
                )
                .collectorName(
                        saleCollectorName(sale)
                )
                .collectorEmail(
                        saleCollectorEmail(sale)
                )
                .updatedByUserId(
                        sale.getUpdatedByUserId()
                )
                .updatedByName(
                        sale.getUpdatedByName()
                )
                .updatedByEmail(
                        sale.getUpdatedByEmail()
                )
                .updatedAt(
                        sale.getUpdatedAt()
                )
                .saleDate(
                        sale.getSaleDate()
                )
                .build();
    }
}
