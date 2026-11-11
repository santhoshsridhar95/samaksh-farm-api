package com.samaksh.farms.sourcing;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.finance.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class SourcingService {
    private final SourcingVendorRepository vendors;
    private final SourcingReceiptRepository receipts;
    private final SourcingPaymentRepository payments;
    private final SourcingRecoveryRepository recoveries;
    private final SourcingSettingsRepository settings;
    private final MoneyService money;
    private final MoneyAccountRepository accounts;
    private final AuditService audit;

    public Map<String,Object> dashboard() {
        money.prepare();
        var allPayments = payments.findAll();
        var allVendors=vendors.findAll();
        var byVendor=new HashMap<Long,SourcingVendor>();allVendors.forEach(v->byVendor.put(v.getId(),v));
        return Map.of("vendors", allVendors, "receipts", receipts.findAllByOrderByDateDescIdDesc().stream().map(r -> view(r, allPayments, byVendor.get(r.getVendorId()))).toList(),
                "payments", allPayments, "recoveries", recoveries.findAll(), "sellingPricePerKg", settings.findById(1L).map(SourcingSettings::getSellingPricePerKg).orElse(BigDecimal.ZERO),
                "accounts", money.accountBalances());
    }
    public SourcingSettings price(SourcingRequests.Price r, Authentication auth) {
        money.prepare(); var s = settings.findById(1L).orElseGet(SourcingSettings::new);
        var old = s.getSellingPricePerKg(); s.setId(1L); s.setSellingPricePerKg(r.sellingPricePerKg()); settings.save(s);
        audit.createAudit(auth, "SOURCING", "UPDATE_SELLING_PRICE", "1", "Common selling price/kg: " + old + " -> " + r.sellingPricePerKg() + "; past receipts unchanged");
        return s;
    }
    public SourcingVendor vendor(Long id, SourcingRequests.Vendor r, Authentication auth) {
        money.prepare();
        if (id == null) { var prior = vendors.findByRequestKey(r.requestKey()); if (prior.isPresent()) {
            if (!Objects.equals(prior.get().getRequestFingerprint(), MoneyService.fingerprint(r))) throw new IllegalArgumentException("This source request was already saved with different details");
            return prior.get();
        } }
        var v = id == null ? new SourcingVendor() : getVendor(id);
        String before = id == null ? "" : vendorSummary(v);
        if (id != null && !v.getKind().equals(r.kind())) throw new IllegalArgumentException("Source type cannot change; create a separate vendor or own-farm source");
        v.setRequestKey(id == null ? r.requestKey() : v.getRequestKey()); v.setName(r.name().trim()); v.setKind(r.kind());
        v.setRequestFingerprint(MoneyService.fingerprint(r));
        v.setPhone(r.phone()); v.setAddress(r.address()); v.setBoxesPerKg(r.boxesPerKg()); v.setCostPerKg(r.costPerKg());
        v.setExpensesBorneBy("OWN_FARM".equals(r.kind()) ? "FARM" : r.expensesBorneBy()); v.setActive(r.active()); vendors.save(v);
        audit.createAudit(auth, "SOURCING_VENDOR", id == null ? "CREATE_VENDOR" : "EDIT_VENDOR", v.getId().toString(), before + " -> " + vendorSummary(v));
        return v;
    }
    public ReceiptView receipt(Long id, SourcingRequests.Receipt r, Authentication auth) {
        money.prepare();
        if (id == null) {
            var prior = receipts.findByRequestKey(r.requestKey());
            if (prior.isPresent()) {
                if (!Objects.equals(prior.get().getRequestFingerprint(), MoneyService.fingerprint(r))) throw new IllegalArgumentException("This receipt request was already saved with different details");
                return view(prior.get(), payments.findAll());
            }
        }
        var vendor = getVendor(r.vendorId());
        if (id == null && !vendor.isActive()) throw new IllegalArgumentException("This source is inactive");
        var receipt = id == null ? new SourcingReceipt() : getReceipt(id);
        if (id != null && r.requestKey().equals(receipt.getLastMutationKey())) {
            if (!Objects.equals(receipt.getRequestFingerprint(), MoneyService.fingerprint(r))) throw new IllegalArgumentException("This receipt edit was already saved with different details");
            return view(receipt, payments.findAll());
        }
        if (id != null && !receipt.getVendorId().equals(r.vendorId())) throw new IllegalArgumentException("A receipt's source cannot change; correct its quantities or prices instead");
        var plannedSellingDate = r.plannedSellingDate() != null ? r.plannedSellingDate() : id == null ? com.samaksh.farms.common.time.BusinessTime.today() : receipt.getPlannedSellingDate();
        if(plannedSellingDate != null && plannedSellingDate.isBefore(r.date()))throw new IllegalArgumentException("Planned selling date cannot be before received date");
        String before = id == null ? "" : receiptSummary(receipt);
        var ratio = id == null ? vendor.getBoxesPerKg() : receipt.getBoxesPerKg();
        var kg = r.unit().equals("KG") ? r.quantity() : r.quantity().divide(ratio, 6, RoundingMode.HALF_UP);
        if (kg.signum() <= 0) throw new IllegalArgumentException("Received weight is too small for this conversion");
        var damagedBoxes = r.damagedBoxes() == null ? BigDecimal.ZERO : r.damagedBoxes();
        var spoiledKg = damagedBoxes.divide(ratio, 6, RoundingMode.HALF_UP);
        if (spoiledKg.compareTo(kg) > 0) throw new IllegalArgumentException("Damaged boxes cannot exceed received stock");
        if (r.soldKg().compareTo(kg.subtract(spoiledKg)) > 0) throw new IllegalArgumentException("Sold kg cannot exceed undamaged received stock");
        var price = id == null ? settings.findById(1L).map(SourcingSettings::getSellingPricePerKg).orElse(BigDecimal.ZERO) : receipt.getSellingPricePerKg();
        if (price.signum() <= 0) throw new IllegalArgumentException("Ask admin to set the common selling price before recording receipts");
        boolean farm = vendor.getKind().equals("OWN_FARM");
        var extra = r.transportCost().add(r.otherCost());
        if (farm && extra.signum() > 0 && (!r.expensesBorneBy().equals("FARM") || !r.expensesPaidBy().equals("FARM")))
            throw new IllegalArgumentException("Own-farm costs must be borne and paid by Samaksh");
        var cost = costs(kg, r.costPerKg(), price, extra, r.expensesBorneBy(), r.expensesPaidBy(), farm, r.soldKg(), spoiledKg);
        var alreadyPaid = paid(id, payments.findAll());
        if (!farm && cost.payable().compareTo(alreadyPaid) < 0 && alreadyPaid.signum() > 0)
            throw new IllegalArgumentException("Payable cannot be reduced below payments already recorded. Correct those payments first.");
        boolean expenseChanged = id == null || !Objects.equals(receipt.getExpenseAccountId(), r.expenseAccountId())
                || !Objects.equals(receipt.getExpensesPaidBy(), r.expensesPaidBy()) || !Objects.equals(receipt.getDate(), r.date())
                || receipt.getTransportCost().add(receipt.getOtherCost()).compareTo(extra) != 0;
        if (expenseChanged && receipt.getExpenseEntryId() != null) money.reverseLinked(receipt.getExpenseEntryId(), "Sourcing receipt correction #" + id, auth);
        receipt.setRequestKey(id == null ? r.requestKey() : receipt.getRequestKey()); receipt.setRequestFingerprint(MoneyService.fingerprint(r));
        receipt.setLastMutationKey(r.requestKey());
        receipt.setVendorId(vendor.getId()); receipt.setDate(r.date()); receipt.setPlannedSellingDate(plannedSellingDate); receipt.setProduct(r.product()); receipt.setQuantityKg(kg);
        receipt.setBoxes(r.unit().equals("BOX") ? r.quantity() : kg.multiply(ratio)); receipt.setBoxesPerKg(ratio);
        receipt.setCostPerKg(r.costPerKg()); receipt.setSellingPricePerKg(price); receipt.setTransportCost(r.transportCost()); receipt.setOtherCost(r.otherCost());
        receipt.setExpensesBorneBy(r.expensesBorneBy()); receipt.setExpensesPaidBy(r.expensesPaidBy()); receipt.setExpenseAccountId(r.expenseAccountId());
        receipt.setSoldKg(r.soldKg()); receipt.setNotes(r.notes()); receipt.setDamagedBoxes(damagedBoxes); receipts.save(receipt);
        receipt.setDamagedBoxes(damagedBoxes);
        if (expenseChanged) {
            receipt.setExpenseEntryId(null);
            if (extra.signum() > 0 && r.expensesPaidBy().equals("FARM")) {
                if (r.expenseAccountId() == null) throw new IllegalArgumentException("Select the account that paid transport / other costs");
                var entry = money.record(new MoneyRequest("source:cost:" + UUID.randomUUID(), "EXPENSE", r.expenseAccountId(), null, extra,
                        "Transport / other costs: " + vendor.getName() + ", receipt #" + receipt.getId() + ", borne by " + r.expensesBorneBy(), null, null, r.date()), auth);
                receipt.setExpenseEntryId(entry.getId());
            }
        }
        audit.createAudit(auth, "SOURCING_RECEIPT", id == null ? "CREATE_RECEIPT" : "EDIT_RECEIPT", receipt.getId().toString(), before + " -> " + receiptSummary(receipt));
        return view(receipt, payments.findAll());
    }
    public List<SourcingPayment> splitPayment(SourcingRequests.SplitPayment request, Authentication auth) {
        money.prepare();
        var r = request.payment();
        var additional = r.additionalAmount() == null ? BigDecimal.ZERO : r.additionalAmount();
        FundingAllocation.validate(request.sources(), r.amount().add(additional));
        String fingerprint = MoneyService.fingerprint(request);
        var prior = new ArrayList<SourcingPayment>();
        for (int i = 0; i < request.sources().size(); i++) {
            var existing = payments.findByRequestKey("split:" + r.requestKey() + ":" + i);
            if (existing.isPresent()) {
                if (!Objects.equals(existing.get().getRequestFingerprint(), fingerprint))
                    throw new IllegalArgumentException("This split vendor payment was already saved with different details");
                prior.add(existing.get());
            }
        }
        if (!prior.isEmpty()) {
            if (prior.size() != request.sources().size()) throw new IllegalArgumentException("Incomplete split payment; refresh before retrying");
            return prior;
        }
        var receipt = getReceipt(r.receiptId());
        if (getVendor(receipt.getVendorId()).getKind().equals("OWN_FARM"))
            throw new IllegalArgumentException("Own-farm receipts do not create vendor payments");
        if (r.amount().signum() < 0 || additional.signum() < 0)
            throw new IllegalArgumentException("Payment amounts cannot be negative");
        validateInvoicePayment(r.amount(), view(receipt, payments.findAll()).balanceDue());
        money.validateFunding(request.sources());
        var invoiceRemaining = r.amount();
        var saved = new ArrayList<SourcingPayment>();
        for (int i = 0; i < request.sources().size(); i++) {
            var source = request.sources().get(i);
            // Sources cover the invoice first, then the recoverable advance, in displayed order.
            var invoice = source.amount().min(invoiceRemaining);
            invoiceRemaining = invoiceRemaining.subtract(invoice);
            var part = payment(null, new SourcingRequests.Payment("split:" + r.requestKey() + ":" + i,
                    r.receiptId(), source.accountId(), invoice, r.date(), r.notes(), source.amount().subtract(invoice)), auth);
            part.setRequestFingerprint(fingerprint);
            saved.add(part);
        }
        return saved;
    }

    public SourcingPayment payment(Long id, SourcingRequests.Payment r, Authentication auth) {
        money.prepare();
        if (id == null) {
            var prior = payments.findByRequestKey(r.requestKey());
            if (prior.isPresent()) {
                if (!Objects.equals(prior.get().getRequestFingerprint(), MoneyService.fingerprint(r))) throw new IllegalArgumentException("Payment request already saved with different details");
                return prior.get();
            }
        }
        var receipt = getReceipt(r.receiptId()); var vendor = getVendor(receipt.getVendorId());
        if (vendor.getKind().equals("OWN_FARM")) throw new IllegalArgumentException("Own-farm receipts do not create vendor payments");
        // A vendor may be paid before delivery; retain the actual business date.
        var payment = id == null ? new SourcingPayment() : payments.findById(id).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (id != null && r.requestKey().equals(payment.getLastMutationKey())) {
            if (!Objects.equals(payment.getRequestFingerprint(), MoneyService.fingerprint(r))) throw new IllegalArgumentException("This payment edit was already saved with different details");
            return payment;
        }
        if (id != null && !payment.getReceiptId().equals(r.receiptId())) throw new IllegalArgumentException("Cannot move a payment to a different receipt");
        var all = payments.findAll().stream().filter(p -> !Objects.equals(p.getId(), id)).toList();
        var due = view(receipt, all).balanceDue();
        validateInvoicePayment(r.amount(), due);
        var additional=r.additionalAmount()==null?BigDecimal.ZERO:r.additionalAmount();
        // A zero-valued edit reverses the linked ledger entries while retaining the payment and audit trail.
        if(r.amount().signum()<0 || additional.signum()<0 || (id==null && r.amount().add(additional).signum()<=0))throw new IllegalArgumentException("Enter an invoice payment or an additional recoverable amount");
        if(id!=null) {
            var recovered=recoveries.findByPaymentId(id);
            var total=recovered.stream().map(SourcingRecovery::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            if(additional.compareTo(total)<0)throw new IllegalArgumentException("Additional amount cannot be less than money already recovered. Edit recoveries first.");
            if(recovered.stream().anyMatch(x->x.getDate().isBefore(r.date())))throw new IllegalArgumentException("Payment date cannot follow a recovery date");
        }
        if (id != null) {
            var refund = payment.getAmount().add(payment.getAdditionalAmount() == null ? BigDecimal.ZERO : payment.getAdditionalAmount());
            if (Objects.equals(payment.getSourceAccountId(), r.sourceAccountId())) refund = refund.subtract(r.amount().add(additional));
            money.validatePersonalRefund(payment.getSourceAccountId(), refund);
        }
        String before = id == null ? "" : "additional " + payment.getAdditionalAmount() + ", amount " + payment.getAmount() + ", source " + payment.getSourceAccountId() + ", date " + payment.getDate();
        if (id != null && payment.getMoneyEntryId()!=null) money.reverseLinked(payment.getMoneyEntryId(), "Vendor payment correction #" + id, auth);
        if (id != null && payment.getAdvanceEntryId()!=null) money.reverseLinked(payment.getAdvanceEntryId(), "Vendor advance correction #" + id, auth);
        var entry = r.amount().signum()==0?null:money.record(new MoneyRequest("source:pay:" + UUID.randomUUID(), "EXPENSE", r.sourceAccountId(), null, r.amount(),
                "Vendor payment: " + vendor.getName() + ", receipt #" + receipt.getId() + (r.notes() == null ? "" : "; " + r.notes()), null, null, r.date()), auth);
        payment.setRequestKey(id == null ? r.requestKey() : payment.getRequestKey()); payment.setRequestFingerprint(MoneyService.fingerprint(r));
        payment.setLastMutationKey(r.requestKey());
        payment.setReceiptId(receipt.getId()); payment.setSourceAccountId(r.sourceAccountId()); payment.setMoneyEntryId(entry==null?null:entry.getId()); payment.setAmount(r.amount()); payment.setDate(r.date()); payment.setNotes(r.notes()); payment.setAdditionalAmount(additional); payment.setAdvanceEntryId(null);
        if(additional.signum()>0)payment.setAdvanceEntryId(money.vendorFunds("source:advance:"+UUID.randomUUID(),r.sourceAccountId(),additional,r.date(),false,"Recoverable vendor advance: "+vendor.getName()+", receipt #"+receipt.getId(),auth).getId());
        payments.save(payment);
        audit.createAudit(auth, "SOURCING_PAYMENT", id == null ? "CREATE_VENDOR_PAYMENT" : "EDIT_VENDOR_PAYMENT", payment.getId().toString(), before + " -> " + vendor.getName() + ", additional recoverable " + additional + ", Rs. " + r.amount() + ", source " + r.sourceAccountId() + ", date " + r.date());
        return payment;
    }
    public SourcingRecovery recovery(Long id, SourcingRequests.Recovery r, Authentication auth) {
        money.prepare();
        if(id==null){var prior=recoveries.findByRequestKey(r.requestKey());if(prior.isPresent()){
            if(!Objects.equals(prior.get().getRequestFingerprint(),MoneyService.fingerprint(r)))throw new IllegalArgumentException("Recovery already saved with different details");return prior.get();}}
        var recovery=id==null?new SourcingRecovery():recoveries.findById(id).orElseThrow(()->new IllegalArgumentException("Recovery not found"));
        if(id!=null&&r.requestKey().equals(recovery.getLastMutationKey())){if(!Objects.equals(recovery.getRequestFingerprint(),MoneyService.fingerprint(r)))throw new IllegalArgumentException("Recovery edit already saved with different details");return recovery;}
        if(id!=null&&!Objects.equals(recovery.getPaymentId(),r.paymentId()))throw new IllegalArgumentException("Cannot move recovery to a different payment");
        var payment=payments.findById(r.paymentId()).orElseThrow(()->new IllegalArgumentException("Payment not found"));
        if(r.date().isBefore(payment.getDate()))throw new IllegalArgumentException("Recovery cannot precede the additional payment");
        var other=recoveries.findByPaymentId(payment.getId()).stream().filter(x->!Objects.equals(x.getId(),id)).map(SourcingRecovery::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        var available=(payment.getAdditionalAmount()==null?BigDecimal.ZERO:payment.getAdditionalAmount()).subtract(other);
        if(r.amount().signum()<=0||r.amount().compareTo(available)>0)throw new IllegalArgumentException("Recovery exceeds the remaining additional amount");
        String before=id==null?"":"amount "+recovery.getAmount()+", date "+recovery.getDate()+", destination "+recovery.getDestinationAccountId();
        if(id!=null)money.reverseLinked(recovery.getMoneyEntryId(),"Vendor recovery correction #"+id,auth);
        var entry=money.vendorFunds("source:recovery:"+UUID.randomUUID(),r.destinationAccountId(),r.amount(),r.date(),true,"Vendor advance recovered, payment #"+payment.getId(),auth);
        recovery.setRequestKey(id==null?r.requestKey():recovery.getRequestKey());recovery.setLastMutationKey(r.requestKey());recovery.setRequestFingerprint(MoneyService.fingerprint(r));
        recovery.setPaymentId(r.paymentId());recovery.setDestinationAccountId(r.destinationAccountId());recovery.setMoneyEntryId(entry.getId());recovery.setAmount(r.amount());recovery.setDate(r.date());recovery.setNotes(r.notes());recoveries.save(recovery);
        audit.createAudit(auth,"SOURCING_RECOVERY",id==null?"CREATE_RECOVERY":"EDIT_RECOVERY",String.valueOf(recovery.getId()),before+" -> "+r);
        return recovery;
    }
    public static Costs costs(BigDecimal kg, BigDecimal costPerKg, BigDecimal sellingPrice, BigDecimal extra,
                               String borneBy, String paidBy, boolean farm, BigDecimal soldKg) {
        return costs(kg, costPerKg, sellingPrice, extra, borneBy, paidBy, farm, soldKg, BigDecimal.ZERO);
    }
    public static Costs costs(BigDecimal kg, BigDecimal costPerKg, BigDecimal sellingPrice, BigDecimal extra,
                               String borneBy, String paidBy, boolean farm, BigDecimal soldKg, BigDecimal spoiledKg) {
        var goods = cash(kg.multiply(costPerKg));
        var deduction = !farm && borneBy.equals("VENDOR") && paidBy.equals("FARM") ? extra : BigDecimal.ZERO;
        var reimbursement = !farm && borneBy.equals("FARM") && paidBy.equals("VENDOR") ? extra : BigDecimal.ZERO;
        var payable = farm ? BigDecimal.ZERO : cash(goods.subtract(deduction).add(reimbursement));
        var landed = cash(goods.add(borneBy.equals("FARM") ? extra : BigDecimal.ZERO));
        var sellable = kg.subtract(spoiledKg);
        var revenue = cash(sellable.multiply(sellingPrice));
        var soldRevenue = cash(soldKg.multiply(sellingPrice));
        var soldCost = sellable.signum() == 0 ? BigDecimal.ZERO : cash(landed.multiply(soldKg).divide(sellable, 6, RoundingMode.HALF_UP));
        return new Costs(goods, deduction, reimbursement, payable, landed, revenue, revenue.subtract(landed), soldRevenue, soldRevenue.subtract(soldCost));
    }
    private ReceiptView view(SourcingReceipt r, List<SourcingPayment> all) {
        return view(r,all,getVendor(r.getVendorId()));
    }
    private ReceiptView view(SourcingReceipt r,List<SourcingPayment> all,SourcingVendor v) {
        var spoiled = (r.getDamagedBoxes() == null ? BigDecimal.ZERO : r.getDamagedBoxes()).divide(r.getBoxesPerKg(), 6, RoundingMode.HALF_UP);
        var c = costs(r.getQuantityKg(), r.getCostPerKg(), r.getSellingPricePerKg(), r.getTransportCost().add(r.getOtherCost()), r.getExpensesBorneBy(), r.getExpensesPaidBy(), v.getKind().equals("OWN_FARM"), r.getSoldKg(), spoiled);
        var paid = paid(r.getId(), all);
        return new ReceiptView(r, v.getName(), v.getKind(), c, paid, c.payable().subtract(paid));
    }
    private void validateInvoicePayment(BigDecimal amount, BigDecimal due) {
        var available = due.max(BigDecimal.ZERO);
        if (amount.compareTo(available) > 0)
            throw new IllegalArgumentException("Invoice payment Rs. " + cash(amount)
                    + " exceeds this receipt's outstanding Rs. " + cash(available)
                    + " after deductions and previous payments. This limit applies to every funding source, including personal funds."
                    + " Enter any vendor advance under Additional paid — recoverable later.");
    }
    private BigDecimal paid(Long id, List<SourcingPayment> all) { return all.stream().filter(p -> id != null && id.equals(p.getReceiptId())).map(SourcingPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private SourcingVendor getVendor(Long id) { return vendors.findById(id).orElseThrow(() -> new IllegalArgumentException("Source not found")); }
    private SourcingReceipt getReceipt(Long id) { return receipts.findById(id).orElseThrow(() -> new IllegalArgumentException("Receipt not found")); }
    private static BigDecimal cash(BigDecimal n) { return n.setScale(2, RoundingMode.HALF_UP); }
    private String vendorSummary(SourcingVendor v) { return v.getName() + ", " + v.getKind() + ", cost/kg " + v.getCostPerKg() + ", boxes/kg " + v.getBoxesPerKg() + ", costs borne by " + v.getExpensesBorneBy() + ", active " + v.isActive(); }
    private String receiptSummary(SourcingReceipt r) { return "source " + r.getVendorId() + ", " + r.getDate() + ", planned selling date " + r.getPlannedSellingDate() + ", " + r.getQuantityKg() + " kg, cost/kg " + r.getCostPerKg() + ", sell/kg " + r.getSellingPricePerKg() + ", transport " + r.getTransportCost() + ", other " + r.getOtherCost() + ", borne " + r.getExpensesBorneBy() + ", paid " + r.getExpensesPaidBy() + ", damaged boxes " + r.getDamagedBoxes() + ", notes " + r.getNotes() + ", sold " + r.getSoldKg() + " kg"; }
    public record Costs(BigDecimal goodsCost, BigDecimal deduction, BigDecimal reimbursement, BigDecimal payable, BigDecimal landedCost, BigDecimal potentialRevenue, BigDecimal potentialMargin, BigDecimal reportedSales, BigDecimal soldMargin) {}
    public record ReceiptView(SourcingReceipt receipt, String vendorName, String kind, Costs costs, BigDecimal paid, BigDecimal balanceDue) {}
}
