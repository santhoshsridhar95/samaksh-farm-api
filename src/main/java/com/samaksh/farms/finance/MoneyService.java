package com.samaksh.farms.finance;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.cashhandover.repo.CashHandoverRepository;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.user.repo.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class MoneyService {
    private final MoneyAccountRepository accounts;
    private final MoneyEntryRepository entries;
    private final SaleRepository sales;
    private final CashHandoverRepository handovers;
    private final UserRepository users;
    private final EntityManager entityManager;
    private final AuditService audit;

    // One transaction-scoped database lock serializes imports, collections and spending
    // across application instances. No balance check can race another debit.
    public void prepare() {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(734215908)").getSingleResult();
        if (accounts.findByAccountKey("FARM_BANK").isPresent()) return;
        account("FARM_BANK", "Samaksh account", "BANK", null);
        account("FARM_SAVINGS", "Farm savings", "SAVINGS", null);
        for (var user : users.findAll()) {
            holder(user.getId(), user.getName(), user.getEmail());
        }
        for (var sale : sales.findAll()) {
            BigDecimal amount = money(sale.getAmountCollected());
            if (amount.signum() <= 0) continue;
            var owner = holder(sale.getCollectorUserId() == null ? sale.getCreatedByUserId() : sale.getCollectorUserId(),
                    sale.getCollectorName() == null ? sale.getCreatedByName() : sale.getCollectorName(),
                    sale.getCollectorEmail() == null ? sale.getCreatedByEmail() : sale.getCollectorEmail());
            add("legacy-sale-" + sale.getId(), "LEGACY_COLLECTION", null, owner.getId(), amount,
                    "Imported sale #" + sale.getId() + "; verify historical collector", null, null,
                    sale.getSaleDate() == null ? BusinessTime.today() : sale.getSaleDate().toLocalDate(), "Migration", sale.getId(), null);
        }
        for (var handover : handovers.findAllByOrderByHandedOverAtDesc()) {
            var source = holder(handover.getCollectorUserId(), handover.getCollectorName(), handover.getCollectorEmail());
            // Historical recipients are free text: keep them separate until explicitly reconciled.
            var destination = account("LEGACY:" + normalized(handover.getOwnerName()),
                    "Historical recipient: " + handover.getOwnerName(), "HISTORICAL", null);
            add("legacy-handover-" + handover.getId(), "LEGACY_TRANSFER", source.getId(), destination.getId(), money(handover.getAmount()),
                    "Imported handover #" + handover.getId() + "; reconcile recipient", null, null,
                    handover.getHandedOverAt() == null ? BusinessTime.today() : handover.getHandedOverAt().toLocalDate(), "Migration", null, null);
        }
    }

    public MoneyAccount holder(Long userId, String name, String email) {
        String key = userId != null && userId > 0 ? "USER:" + userId : "LEGACY_HOLDER:" + normalized(email == null ? name : email);
        return account(key, name == null || name.isBlank() ? "Unassigned collector" : name, "HELD", userId);
    }

    private MoneyAccount account(String key, String name, String kind, Long userId) {
        return accounts.findByAccountKey(key).orElseGet(() -> {
            var value = new MoneyAccount();
            value.setAccountKey(key); value.setName(name); value.setKind(kind); value.setUserId(userId);
            var saved = accounts.save(value);
            audit.createAudit(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication(),
                    "MONEY_ACCOUNT", "CREATE_ACCOUNT", saved.getId().toString(), "Created " + kind + " account: " + name);
            return saved;
        });
    }

    public void collection(Long userId, String name, String email, double value, Long saleId, Authentication auth) {
        prepare();
        BigDecimal amount = money(value);
        if (amount.signum() == 0) return;
        if (amount.signum() < 0) throw new IllegalArgumentException("Collections cannot be reduced. Contact a manager to reconcile the original payment.");
        var destination = holder(userId, name, email);
        add(UUID.randomUUID().toString(), "COLLECTION", null, destination.getId(), amount,
                "Sale #" + saleId + " collection", null, null, BusinessTime.today(), auth.getName(), saleId, null);
    }

    public void correctCollection(Long saleId,Long userId,String name,String email,double difference,LocalDate date,Authentication auth) {
        prepare();var delta=money(difference);if(delta.signum()==0)return;
        var journal=entries.findAll();
        // The first cash entry identifies the original collector even if later payments changed the sale's collector fields.
        Long originalHolder=journal.stream().filter(e->Objects.equals(e.getSaleId(),saleId)&&Set.of("COLLECTION","LEGACY_COLLECTION").contains(e.getType())&&e.getDestinationId()!=null)
            .min(Comparator.comparing(MoneyEntry::getId)).map(MoneyEntry::getDestinationId).orElse(null);
        var holder=originalHolder==null?holder(userId,name,email):getAccount(originalHolder);
        if(delta.signum()<0 && balances(journal).getOrDefault(holder.getId(),BigDecimal.ZERO).compareTo(delta.abs())<0)
            throw new IllegalArgumentException("The collector has already spent or transferred this money. Return sufficient funds to their held-money account before reducing this receipt.");
        var entry=add(UUID.randomUUID().toString(),"COLLECTION_CORRECTION",delta.signum()<0?holder.getId():null,delta.signum()>0?holder.getId():null,delta.abs(),"Receipt correction for sale #"+saleId,null,null,date,auth.getName(),saleId,null);
        audit.createAudit(auth,"MONEY","CORRECT_COLLECTION",entry.getId().toString(),"Sale #"+saleId+"; cash adjustment "+delta+"; holder "+holder.getName());
    }

    public Map<String, Object> dashboard() {
        var allAccounts = availableAccounts();
        var allEntries = entries.findAllByOrderByDateDescIdDesc();
        var balances = balances(allEntries);
        var accountViews = allAccounts.stream().map(a -> new AccountView(a.getId(), a.getName(), a.getKind(),
                balances.getOrDefault(a.getId(), BigDecimal.ZERO))).toList();
        return Map.of("accounts", accountViews, "entries", allEntries,
                "reconciliationRequired", allEntries.stream().anyMatch(e -> e.getType().startsWith("LEGACY")));
    }

    public List<AccountView> accountBalances() {
        var allAccounts=availableAccounts();
        var totals=balances(entries.findAll());
        return allAccounts.stream().map(a->new AccountView(a.getId(),a.getName(),a.getKind(),totals.getOrDefault(a.getId(),BigDecimal.ZERO))).toList();
    }

    public List<MoneyAccount> availableAccounts() {
        prepare();
        var all=new ArrayList<>(accounts.findAll());
        var keys=new HashSet<String>();all.forEach(a->keys.add(a.getAccountKey()));
        for (var user : users.findAll()) if (Boolean.TRUE.equals(user.getActive())) {
            if(keys.add("USER:"+user.getId()))all.add(holder(user.getId(),user.getName(),user.getEmail()));
            if(keys.add("PERSONAL_USER:"+user.getId()))all.add(account("PERSONAL_USER:"+user.getId(),user.getName()+" — Personal pocket","PERSONAL",user.getId()));
        }
        return all;
    }

    public MoneyAccount createAccount(String name, String kind) {
        prepare();
        if (name == null || name.isBlank() || name.length() > 150 || kind == null || !Set.of("HELD", "PERSONAL", "BANK", "SAVINGS").contains(kind))
            throw new IllegalArgumentException("Enter a name and valid account type");
        String key = "CUSTOM:" + kind + ":" + normalized(name);
        if (accounts.findByAccountKey(key).isPresent() || accounts.findAll().stream().anyMatch(a -> a.getKind().equals(kind) && normalized(a.getName()).equals(normalized(name))))
            throw new IllegalArgumentException("This account already exists. Select the existing account.");
        return account(key, name.trim(), kind, null);
    }

    public MoneyEntry record(MoneyRequest r, Authentication auth) {
        prepare();
        var previous = entries.findByRequestKey(r.requestKey());
        if (previous.isPresent()) {
            if (!Objects.equals(previous.get().getRequestFingerprint(), fingerprint(r)))
                throw new IllegalArgumentException("This request was already saved with different details. Refresh and start a new transaction.");
            return previous.get();
        }
        validateShape(r);
        if("REIMBURSEMENT".equals(r.type()))validateReimbursement(r,entries.findAll());
        MoneyAccount source = r.sourceId() == null ? null : getAccount(r.sourceId());
        MoneyAccount destination = r.destinationId() == null ? null : getAccount(r.destinationId());
        if (source != null && !source.getKind().equals("PERSONAL")) requireFunds(source.getId(), r.amount());
        var saved = add(r.requestKey(), r.type(), r.sourceId(), r.destinationId(), r.amount(), r.description(), r.payee(), r.salaryMonth(), r.date(), auth.getName(), null, null);
        saved.setRequestFingerprint(fingerprint(r));
        linkEmployee(saved, r);
        audit.createAudit(auth, "MONEY", "RECORD_" + r.type(), saved.getId().toString(),
                r.description() + ": Rs. " + r.amount() + ", from: " + (source == null ? "Outside funds" : source.getName())
                        + ", to: " + (destination == null ? r.payee() : destination.getName()) + ", date: " + r.date()
                        + ", employeeId: " + r.employeeId() + (r.salaryMonth() == null ? "" : ", salary month: " + r.salaryMonth()));
        return saved;
    }

    private void validateEmployee(MoneyRequest r) {
        if(r.employeeId()==null)return; // Preserve unlinked historical payments.
        if(!Set.of("SALARY","EXPENSE").contains(r.type()))throw new IllegalArgumentException("Only salary and expense entries can be linked to an employee");
        var employee=entityManager.find(com.samaksh.farms.employee.Employee.class,r.employeeId());
        if(employee==null)throw new IllegalArgumentException("Employee not found");
        if("SALARY".equals(r.type())){
            YearMonth month;
            try{month=YearMonth.parse(r.salaryMonth());}catch(Exception ex){throw new IllegalArgumentException("Salary month is required (YYYY-MM)");}
            if(month.atEndOfMonth().isBefore(employee.getJoinedOn())||(employee.getLeftOn()!=null&&month.atDay(1).isAfter(employee.getLeftOn())))throw new IllegalArgumentException("Salary month must fall within employment dates");
        }
    }
    private void linkEmployee(MoneyEntry entry,MoneyRequest r){
        entry.setEmployeeId(r.employeeId());
        if(r.employeeId()!=null)entry.setPayee(entityManager.find(com.samaksh.farms.employee.Employee.class,r.employeeId()).getName());
    }
    private void validateShape(MoneyRequest r) {
        if (!Set.of("EXPENSE", "SALARY", "TRANSFER", "REIMBURSEMENT", "OPENING", "CONTRIBUTION").contains(r.type()))
            throw new IllegalArgumentException("Invalid transaction type");
        MoneyAccount source = r.sourceId() == null ? null : getAccount(r.sourceId());
        MoneyAccount destination = r.destinationId() == null ? null : getAccount(r.destinationId());
        validateEmployee(r);
        boolean incoming = Set.of("OPENING", "CONTRIBUTION").contains(r.type());
        if (incoming && (destination == null || destination.getKind().equals("PERSONAL") ||
                (source != null && (!r.type().equals("CONTRIBUTION") || !source.getKind().equals("PERSONAL")))))
            throw new IllegalArgumentException("Choose a farm account to receive the money");
        if (!incoming && source == null) throw new IllegalArgumentException("Choose the account paying this amount");
        if (r.type().equals("TRANSFER") && (destination == null || destination.getKind().equals("PERSONAL") || source.getKind().equals("PERSONAL") || source.getId().equals(destination.getId())))
            throw new IllegalArgumentException("Transfers require two different farm money accounts; use contribution for personal funds");
        if (Set.of("EXPENSE", "SALARY").contains(r.type()) && destination != null)
            throw new IllegalArgumentException("Expenses and salaries cannot credit another farm account");
        if (r.type().equals("REIMBURSEMENT") && (destination == null || !destination.getKind().equals("PERSONAL") || source.getKind().equals("PERSONAL")))
            throw new IllegalArgumentException("Reimbursements must move farm money to a personal account");
        if (r.type().equals("SALARY")) {
            if (r.payee() == null || r.payee().isBlank()) throw new IllegalArgumentException("Employee name is required");
            try { YearMonth.parse(r.salaryMonth()); } catch (Exception e) { throw new IllegalArgumentException("Salary month is required (YYYY-MM)"); }
        }
    }

    private void validateReimbursement(MoneyRequest r,List<MoneyEntry> journal) {
        var currentOwed=balances(journal).getOrDefault(r.destinationId(),BigDecimal.ZERO).negate();
        var datedOwed=balances(journal.stream().filter(e->!e.getDate().isAfter(r.date())).toList()).getOrDefault(r.destinationId(),BigDecimal.ZERO).negate();
        if(r.amount().compareTo(currentOwed.min(datedOwed))>0)
            throw new IllegalArgumentException("Reimbursement exceeds the personal amount owed. Check the payment date and previous repayments.");
    }

    public record EditRequest(@jakarta.validation.Valid @jakarta.validation.constraints.NotNull MoneyRequest entry,
                              @jakarta.validation.constraints.NotNull Long expectedRevision) {}

    public MoneyEntry edit(Long id, EditRequest request, Authentication auth) {
        prepare();
        var original = entries.findById(id).orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        var r = request.entry();
        if (Objects.equals(original.getLastEditRequestKey(), r.requestKey())) {
            if (!Objects.equals(original.getLastEditFingerprint(), fingerprint(r))) throw new IllegalArgumentException("This edit was already saved with different details");
            return original;
        }
        long revision = original.getRevision() == null ? 0 : original.getRevision();
        if (!Objects.equals(request.expectedRevision(), revision)) throw new IllegalArgumentException("This transaction changed since you opened it. Refresh and reopen Edit.");
        if (original.getRequestKey().startsWith("source:")) throw new IllegalArgumentException("Edit vendor-linked transactions in Sourcing");
        if (!Set.of("EXPENSE", "SALARY", "TRANSFER", "REIMBURSEMENT", "OPENING", "CONTRIBUTION", "VENDOR_ADVANCE", "VENDOR_RECOVERY").contains(original.getType()) || entries.existsByReversalOf(id))
            throw new IllegalArgumentException("Only active expense, salary, transfer, contribution and opening entries can be edited here");
        validateShape(r);
        var all = entries.findAll();
        if("REIMBURSEMENT".equals(r.type()))validateReimbursement(r,all.stream().filter(e->!e.getId().equals(id)).toList());
        var beforeBalances = balances(all);
        var candidate = new MoneyEntry(); candidate.setSourceId(r.sourceId()); candidate.setDestinationId(r.destinationId()); candidate.setAmount(r.amount());
        var proposed = new ArrayList<>(all.stream().filter(e -> !e.getId().equals(id)).toList()); proposed.add(candidate);
        var afterBalances = balances(proposed);
        Set<Long> touched = new HashSet<>();
        Collections.addAll(touched, original.getSourceId(), original.getDestinationId(), r.sourceId(), r.destinationId()); touched.remove(null);
        for (var accountId : touched) {
            var balance = afterBalances.getOrDefault(accountId, BigDecimal.ZERO);
            if (getAccount(accountId).getKind().equals("PERSONAL") && balance.signum()>0 && balance.compareTo(beforeBalances.getOrDefault(accountId,BigDecimal.ZERO))>0)
                throw new IllegalArgumentException("This edit exceeds the amount owed to "+getAccount(accountId).getName()+". Correct its reimbursements first.");
            if (!getAccount(accountId).getKind().equals("PERSONAL") && balance.signum() < 0 && balance.compareTo(beforeBalances.getOrDefault(accountId, BigDecimal.ZERO)) < 0)
                throw new IllegalArgumentException("This edit would overdraw " + getAccount(accountId).getName() + ". Check money already spent or transferred.");
        }
        String before = entryDetails(original);
        original.setType(r.type()); original.setSourceId(r.sourceId()); original.setDestinationId(r.destinationId()); original.setAmount(r.amount());
        original.setDate(r.date()); original.setDescription(r.description()); original.setPayee(r.payee()); original.setSalaryMonth(r.salaryMonth());
        linkEmployee(original, r);
        original.setRevision(revision + 1); original.setLastEditRequestKey(r.requestKey()); original.setLastEditFingerprint(fingerprint(r));
        original.setUpdatedAt(BusinessTime.now()); original.setUpdatedBy(auth.getName());
        entries.save(original);
        audit.createAudit(auth, "MONEY", "EDIT_TRANSACTION", id.toString(), "Before: " + before + " | After: " + entryDetails(original));
        return original;
    }

    private String entryDetails(MoneyEntry e) {
        return "type=" + e.getType() + ", amount=" + e.getAmount() + ", date=" + e.getDate()
                + ", from=" + e.getSourceId() + ", to=" + e.getDestinationId() + ", payee=" + e.getPayee()
                + ", employeeId=" + e.getEmployeeId() + ", salaryMonth=" + e.getSalaryMonth() + ", purpose=" + e.getDescription();
    }

    public MoneyEntry settle(SettlementRequest request, Authentication auth) {
        prepare();
        var previous = entries.findByRequestKey("settle:" + request.requestKey());
        if (previous.isPresent()) {
            if (!Objects.equals(previous.get().getSourceId(), request.sourceId()) || !Objects.equals(previous.get().getDestinationId(), request.destinationId()))
                throw new IllegalArgumentException("This settlement request was already used for different accounts");
            return previous.get();
        }
        var source = getAccount(request.sourceId());
        var destination = getAccount(request.destinationId());
        if (source.getKind().equals("PERSONAL") || destination.getKind().equals("PERSONAL") || source.getId().equals(destination.getId()))
            throw new IllegalArgumentException("Choose two different farm accounts or holders to transfer the full balance");
        var amount = balances(entries.findAll()).getOrDefault(source.getId(), BigDecimal.ZERO);
        if (amount.signum() <= 0) throw new IllegalArgumentException("There is no positive balance to settle");
        return record(new MoneyRequest("settle:" + request.requestKey(), "TRANSFER", source.getId(), destination.getId(), amount,
                "Full balance settlement: " + source.getName() + " to " + destination.getName(), null, null, BusinessTime.today()), auth);
    }

    public record SettlementRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=70) String requestKey,
                                    @jakarta.validation.constraints.NotNull Long sourceId, @jakarta.validation.constraints.NotNull Long destinationId) {}

    public record ReconciliationRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=70) String requestKey,
            @jakarta.validation.constraints.NotNull Long accountId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.Digits(integer=12,fraction=2) BigDecimal actualBalance,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Digits(integer=12,fraction=2) BigDecimal expectedBalance,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=900) String reason) {}

    public MoneyEntry reconcile(ReconciliationRequest r, Authentication auth) {
        prepare(); // Same database lock as collections, imports and spending.
        String key = "reconcile:" + r.requestKey();
        var previous = entries.findByRequestKey(key);
        if (previous.isPresent()) {
            if (!Objects.equals(previous.get().getRequestFingerprint(), fingerprint(r)))
                throw new IllegalArgumentException("This balance correction was already saved with different details");
            return previous.get();
        }
        var account = getAccount(r.accountId());
        if (account.getKind().equals("PERSONAL")) throw new IllegalArgumentException("Set actual balances for farm money holders, bank or savings accounts. Personal amounts represent money owed, not cash available.");
        if (r.actualBalance() == null || r.actualBalance().signum() < 0) throw new IllegalArgumentException("Actual money held cannot be negative");
        var before = balances(entries.findAll()).getOrDefault(account.getId(), BigDecimal.ZERO);
        if (r.expectedBalance() == null || before.compareTo(r.expectedBalance()) != 0)
            throw new IllegalArgumentException("This account balance changed. Refresh and check the actual amount again.");
        var difference = r.actualBalance().subtract(before);
        var saved = add(key, "BALANCE_ADJUSTMENT", difference.signum() < 0 ? account.getId() : null,
                difference.signum() >= 0 ? account.getId() : null, difference.abs(),
                "Actual balance set: " + before + " -> " + r.actualBalance() + ". " + r.reason().trim(),
                null, null, BusinessTime.today(), auth.getName(), null, null);
        saved.setRequestFingerprint(fingerprint(r));
        audit.createAudit(auth, "MONEY", "SET_ACTUAL_BALANCE", saved.getId().toString(),
                "Account #" + account.getId() + " (" + account.getName() + "): before=" + before + ", actual=" + r.actualBalance() + ", adjustment=" + difference + "; reason=" + r.reason());
        return saved;
    }

    public MoneyEntry reverse(Long id, String reason, Authentication auth) {
        return reverseEntry(id, reason, auth, false);
    }

    public MoneyEntry vendorFunds(String key, Long accountId, BigDecimal amount, LocalDate date, boolean recovery, String description, Authentication auth) {
        prepare();
        var previous=entries.findByRequestKey(key);if(previous.isPresent())return previous.get();
        if(amount==null||amount.signum()<=0)throw new IllegalArgumentException("Amount must be positive");
        var account=getAccount(accountId);
        if(!recovery&&!account.getKind().equals("PERSONAL"))requireFunds(accountId,amount);
        var saved=add(key,recovery?"VENDOR_RECOVERY":"VENDOR_ADVANCE",recovery?null:accountId,recovery?accountId:null,amount,description,null,null,date,auth.getName(),null,null);
        audit.createAudit(auth,"MONEY",saved.getType(),saved.getId().toString(),description+"; amount "+amount+"; account "+account.getName()+"; date "+date);
        return saved;
    }
    public MoneyEntry reverseLinked(Long id, String reason, Authentication auth) {
        return reverseEntry(id, reason, auth, true);
    }

    private MoneyEntry reverseEntry(Long id, String reason, Authentication auth, boolean linked) {
        prepare();
        if (reason == null || reason.isBlank() || reason.length() > 900) throw new IllegalArgumentException("A correction reason is required (up to 900 characters)");
        var original = entries.findById(id).orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (original.getRequestKey().startsWith("source:") && !linked)
            throw new IllegalArgumentException("Edit this payment in Sourcing so the vendor balance and money ledger stay consistent");
        if (!Set.of("EXPENSE", "SALARY", "TRANSFER", "REIMBURSEMENT", "OPENING", "CONTRIBUTION", "VENDOR_ADVANCE", "VENDOR_RECOVERY").contains(original.getType()) || entries.existsByReversalOf(id))
            throw new IllegalArgumentException("This transaction cannot be reversed");
        if (original.getDestinationId() != null && !getAccount(original.getDestinationId()).getKind().equals("PERSONAL")) requireFunds(original.getDestinationId(), original.getAmount());
        var saved = add("reversal-" + id, "REVERSAL", original.getDestinationId(), original.getSourceId(), original.getAmount(),
                reason.trim(), original.getPayee(), original.getSalaryMonth(), BusinessTime.today(), auth.getName(), null, id);
        saved.setEmployeeId(original.getEmployeeId());
        audit.createAudit(auth, "MONEY", "REVERSE", id.toString(), "Reversed " + original.getType() + " Rs. " + original.getAmount()
                + "; reversal entry #" + saved.getId() + "; reason: " + reason);
        return saved;
    }

    private MoneyAccount getAccount(Long id) {
        return accounts.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }
    private void requireFunds(Long accountId, BigDecimal amount) {
        var balance = balances(entries.findAll()).getOrDefault(accountId, BigDecimal.ZERO);
        if (balance.compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient farm money in the selected account. Check collections and opening balances.");
    }
    static Map<Long, BigDecimal> balances(List<MoneyEntry> entries) {
        Map<Long, BigDecimal> result = new HashMap<>();
        for (var e : entries) {
            if (e.getSourceId() != null) result.merge(e.getSourceId(), e.getAmount().negate(), BigDecimal::add);
            if (e.getDestinationId() != null) result.merge(e.getDestinationId(), e.getAmount(), BigDecimal::add);
        }
        return result;
    }
    private MoneyEntry add(String key, String type, Long source, Long destination, BigDecimal amount, String description,
                           String payee, String month, LocalDate date, String actor, Long saleId, Long reversalOf) {
        var e = new MoneyEntry();
        e.setRequestKey(key); e.setType(type); e.setSourceId(source); e.setDestinationId(destination); e.setAmount(amount);
        e.setDescription(description); e.setPayee(payee); e.setSalaryMonth(month); e.setDate(date);
        e.setRecordedBy(actor); e.setRecordedAt(BusinessTime.now()); e.setSaleId(saleId); e.setReversalOf(reversalOf);
        return entries.save(e);
    }
    static BigDecimal money(Double amount) {
        if (amount == null) return BigDecimal.ZERO;
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Amount must be finite");
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
    private String normalized(String text) { return String.valueOf(text).trim().toLowerCase(Locale.ROOT); }
    public static String fingerprint(Object request) {
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(request.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public record AccountView(Long id, String name, String kind, BigDecimal balance) {}
}
