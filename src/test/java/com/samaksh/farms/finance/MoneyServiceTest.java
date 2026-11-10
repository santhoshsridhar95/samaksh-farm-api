package com.samaksh.farms.finance;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.cashhandover.repo.CashHandoverRepository;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.user.repo.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.*;
import org.springframework.security.core.Authentication;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MoneyServiceTest {
    MoneyAccountRepository accounts;
    MoneyEntryRepository entries;
    MoneyService service;
    Authentication auth;
    EntityManager em;
    List<MoneyEntry> journal;
    List<MoneyAccount> wallets;
    AuditService audit;

    @BeforeEach void setup() {
        accounts = mock(MoneyAccountRepository.class); entries = mock(MoneyEntryRepository.class);
        em = mock(EntityManager.class); var query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        auth = mock(Authentication.class); when(auth.getName()).thenReturn("Santosh");
        audit = mock(AuditService.class);
        service = new MoneyService(accounts, entries, mock(SaleRepository.class), mock(CashHandoverRepository.class), mock(UserRepository.class), em, audit);
        journal = new ArrayList<>(); wallets = new ArrayList<>();
        wallet(1L, "USER:1", "Santosh", "HELD"); wallet(2L, "USER:2", "Sandeep", "HELD");
        wallet(3L, "FARM_BANK", "Samaksh", "BANK"); wallet(4L, "POCKET:1", "Santosh pocket", "PERSONAL");
        when(accounts.findByAccountKey(anyString())).thenAnswer(i -> wallets.stream().filter(a -> a.getAccountKey().equals(i.getArgument(0))).findFirst());
        when(accounts.findById(anyLong())).thenAnswer(i -> wallets.stream().filter(a -> a.getId().equals(i.getArgument(0))).findFirst());
        when(entries.findAll()).thenAnswer(i -> new ArrayList<>(journal));
        when(entries.findById(anyLong())).thenAnswer(i -> journal.stream().filter(e -> e.getId().equals(i.getArgument(0))).findFirst());
        when(entries.findByRequestKey(anyString())).thenAnswer(i -> journal.stream().filter(e -> e.getRequestKey().equals(i.getArgument(0))).findFirst());
        when(entries.existsByReversalOf(anyLong())).thenAnswer(i -> journal.stream().anyMatch(e -> Objects.equals(e.getReversalOf(), i.getArgument(0))));
        when(entries.save(any())).thenAnswer(i -> { MoneyEntry e = i.getArgument(0); if (e.getId() == null) { e.setId((long) journal.size()+1); journal.add(e); } return e; });
    }
    @Test void employeeSalaryLinksSurviveReversalAndRetries() {
        var employee=new com.samaksh.farms.employee.Employee();employee.setId(7L);employee.setName("Founder");employee.setJoinedOn(LocalDate.of(2026,1,1));
        when(em.find(com.samaksh.farms.employee.Employee.class,7L)).thenReturn(employee);
        var request=new MoneyRequest("employee-pay","SALARY",4L,null,new BigDecimal("500"),"Partial salary","Ignored name","2026-11",LocalDate.of(2026,11,8),7L);
        var saved=service.record(request,auth);service.record(request,auth);
        assertEquals(1,journal.size());assertEquals(7L,saved.getEmployeeId());assertEquals("Founder",saved.getPayee());
        var reversal=service.reverse(saved.getId(),"Correction",auth);assertEquals(7L,reversal.getEmployeeId());assertEquals("2026-11",reversal.getSalaryMonth());assertEquals(0,balance(4L).signum());
    }
    @Test void unknownEmployeesAndSalaryOutsideEmploymentAreRejected() {
        var request=new MoneyRequest("bad-employee","SALARY",4L,null,new BigDecimal("500"),"Salary","Name","2026-01",LocalDate.of(2026,11,8),7L);
        assertThrows(IllegalArgumentException.class,()->service.record(request,auth));
        var employee=new com.samaksh.farms.employee.Employee();employee.setJoinedOn(LocalDate.of(2026,2,1));
        when(em.find(com.samaksh.farms.employee.Employee.class,7L)).thenReturn(employee);
        assertThrows(IllegalArgumentException.class,()->service.record(request,auth));assertTrue(journal.isEmpty());
    }
    void wallet(Long id, String key, String name, String kind) {
        var a = new MoneyAccount(); a.setId(id); a.setAccountKey(key); a.setName(name); a.setKind(kind); wallets.add(a);
    }
    MoneyRequest request(String type, Long source, Long destination, String amount) {
        return new MoneyRequest(UUID.randomUUID().toString(), type, source, destination, new BigDecimal(amount), "Farm payment", "Yogesh", "2026-11", LocalDate.of(2026,11,8));
    }
    BigDecimal balance(Long id) { return MoneyService.balances(journal).getOrDefault(id, BigDecimal.ZERO); }
    @Test void fiveHundredCollectionTransferredToSandeepConservesFarmMoney() {
        service.collection(1L, "Santosh", null, 500, 10L, auth);
        service.record(request("TRANSFER", 1L, 2L, "500"), auth);
        assertEquals(0, balance(1L).compareTo(BigDecimal.ZERO));
        assertEquals(0, balance(2L).compareTo(new BigDecimal("500")));
    }
    @Test void salaryFromSalesDeductsOnlySelectedHolder() {
        service.collection(1L, "Santosh", null, 5000, 10L, auth);
        service.record(request("SALARY", 1L, null, "5000"), auth);
        assertEquals(0, balance(1L).signum()); assertEquals(0, balance(2L).signum());
        assertEquals("Yogesh", journal.get(1).getPayee()); assertEquals("2026-11", journal.get(1).getSalaryMonth());
    }
    @Test void personalSalaryDoesNotSpendFarmCollections() {
        service.collection(1L, "Santosh", null, 5000, 10L, auth);
        service.record(request("SALARY", 4L, null, "5000"), auth);
        assertEquals(0, balance(1L).compareTo(new BigDecimal("5000")));
        assertEquals(0, balance(4L).compareTo(new BigDecimal("-5000")));
    }
    @Test void overspendingAndSelfTransfersAreRejected() {
        service.collection(1L, "Santosh", null, 500, 10L, auth);
        assertThrows(IllegalArgumentException.class, () -> service.record(request("EXPENSE", 1L, null, "500.01"), auth));
        assertThrows(IllegalArgumentException.class, () -> service.record(request("TRANSFER", 1L, 1L, "100"), auth));
        assertEquals(1, journal.size());
    }
    @Test void duplicateRequestDoesNotSpendTwice() {
        service.collection(1L, "Santosh", null, 500, 10L, auth);
        var r = request("EXPENSE", 1L, null, "100");
        service.record(r, auth); service.record(r, auth);
        assertEquals(2, journal.size()); assertEquals(0, balance(1L).compareTo(new BigDecimal("400")));
    }
    @Test void reversingExpenseRestoresBalanceAndPreservesOriginal() {
        service.collection(1L, "Santosh", null, 500, 10L, auth);
        var expense = service.record(request("EXPENSE", 1L, null, "100"), auth);
        service.reverse(expense.getId(), "Duplicate invoice", auth);
        assertEquals(3, journal.size()); assertEquals(0, balance(1L).compareTo(new BigDecimal("500")));
        assertThrows(IllegalArgumentException.class, () -> service.reverse(expense.getId(), "Again", auth));
    }
    @Test void partialCollectionsStayWithEachActualCollector() {
        service.collection(1L, "Santosh", null, 200, 10L, auth);
        service.collection(2L, "Sandeep", null, 300, 10L, auth);
        assertEquals(0, balance(1L).compareTo(new BigDecimal("200")));
        assertEquals(0, balance(2L).compareTo(new BigDecimal("300")));
    }
    @Test void decimalAmountsAreExact() {
        service.collection(1L, "Santosh", null, 0.1, 10L, auth);
        service.collection(1L, "Santosh", null, 0.2, 10L, auth);
        service.record(request("EXPENSE", 1L, null, "0.30"), auth);
        assertEquals(0, balance(1L).signum());
    }
    @Test void spentTransferCannotBeReversedToCreateNegativeBalance() {
        service.collection(1L, "Santosh", null, 500, 10L, auth);
        var transfer = service.record(request("TRANSFER", 1L, 2L, "500"), auth);
        service.record(request("SALARY", 2L, null, "500"), auth);
        assertThrows(IllegalArgumentException.class, () -> service.reverse(transfer.getId(), "Wrong holder", auth));
    }
    @Test void fullSettlementClearsHolderAndRetryDoesNotTransferNewReceipts() {
        service.collection(1L, "Santosh", null, 500, 10L, auth);
        var r = new MoneyService.SettlementRequest("settle-1", 1L, 3L);
        service.settle(r, auth);
        assertEquals(0, balance(1L).signum());
        assertEquals(0, balance(3L).compareTo(new BigDecimal("500")));
        service.collection(1L, "Santosh", null, 200, 11L, auth);
        service.settle(r, auth);
        assertEquals(0, balance(1L).compareTo(new BigDecimal("200")));
    }
    @Test void actualBalanceCanClearHistoricalMoneyAndFutureReceiptsAccumulate() {
        service.collection(1L,"Santosh",null,15000,10L,auth);
        var request = new MoneyService.ReconciliationRequest("initial",1L,BigDecimal.ZERO,new BigDecimal("15000"),"Spent before app tracking");
        var adjustment = service.reconcile(request,auth);
        assertEquals("BALANCE_ADJUSTMENT",adjustment.getType());
        assertEquals(0,balance(1L).signum());
        assertEquals(2,journal.size());assertEquals(new BigDecimal("15000.00"),journal.getFirst().getAmount());
        service.collection(1L,"Santosh",null,300,11L,auth);
        service.reconcile(request,auth);
        assertEquals(3,journal.size());assertEquals(0,balance(1L).compareTo(new BigDecimal("300")));
        assertThrows(IllegalArgumentException.class,()->service.reconcile(new MoneyService.ReconciliationRequest("initial",1L,BigDecimal.ONE,new BigDecimal("15000"),"Changed"),auth));
    }
    @Test void reconciliationSetsExactAmountAndRejectsStaleOrPersonalBalances() {
        service.reconcile(new MoneyService.ReconciliationRequest("start",2L,new BigDecimal("250"),BigDecimal.ZERO,"Cash counted"),auth);
        assertEquals(0,balance(2L).compareTo(new BigDecimal("250")));
        assertThrows(IllegalArgumentException.class,()->service.reconcile(new MoneyService.ReconciliationRequest("stale",2L,BigDecimal.ZERO,BigDecimal.ZERO,"Cash counted"),auth));
        assertThrows(IllegalArgumentException.class,()->service.reconcile(new MoneyService.ReconciliationRequest("pocket",4L,BigDecimal.ZERO,BigDecimal.ZERO,"Cash counted"),auth));
        assertThrows(IllegalArgumentException.class,()->service.reconcile(new MoneyService.ReconciliationRequest("negative",2L,BigDecimal.ONE.negate(),new BigDecimal("250"),"Cash counted"),auth));
        service.reconcile(new MoneyService.ReconciliationRequest("same",2L,new BigDecimal("250"),new BigDecimal("250"),"Verified unchanged"),auth);
        assertEquals(0,balance(2L).compareTo(new BigDecimal("250")));
    }
    @Test void fullBalanceTransfersToAnotherHolderWithoutBankAndRejectsSameAccount() {
        service.collection(1L,"Santosh",null,500,10L,auth);
        assertThrows(IllegalArgumentException.class,()->service.settle(new MoneyService.SettlementRequest("same",1L,1L),auth));
        assertThrows(IllegalArgumentException.class,()->service.settle(new MoneyService.SettlementRequest("personal",1L,4L),auth));
        service.settle(new MoneyService.SettlementRequest("sandeep",1L,2L),auth);
        assertEquals(0,balance(1L).signum());assertEquals(0,balance(2L).compareTo(new BigDecimal("500")));
    }
    @Test void personalContributionFundsHolderAndCanBeRepaidWithoutExtraExpense() {
        var contribution=request("CONTRIBUTION",4L,2L,"500");
        service.record(contribution,auth);service.record(contribution,auth);
        assertEquals(1,journal.size());assertEquals(0,balance(2L).compareTo(new BigDecimal("500")));assertEquals(0,balance(4L).compareTo(new BigDecimal("-500")));
        service.record(request("REIMBURSEMENT",2L,4L,"200"),auth);
        assertEquals(0,balance(2L).compareTo(new BigDecimal("300")));assertEquals(0,balance(4L).compareTo(new BigDecimal("-300")));
        assertThrows(IllegalArgumentException.class,()->service.record(request("CONTRIBUTION",1L,2L,"100"),auth));
        assertThrows(IllegalArgumentException.class,()->service.record(request("CONTRIBUTION",4L,4L,"100"),auth));
    }
    @Test void editingExpenseUpdatesSameRowAndPreservesBeforeAfterAudit() {
        service.collection(1L, "Santosh", null, 500, 10L, auth);
        var original = service.record(request("EXPENSE", 1L, null, "100"), auth);
        var updated = request("SALARY", 4L, null, "200");
        service.edit(original.getId(), new MoneyService.EditRequest(updated, 0L), auth);
        assertEquals(2, journal.size());
        assertEquals("SALARY", original.getType());
        assertEquals(1L, original.getRevision());
        assertEquals(0, balance(1L).compareTo(new BigDecimal("500")));
        assertEquals(0, balance(4L).compareTo(new BigDecimal("-200")));
        verify(audit).createAudit(eq(auth), eq("MONEY"), eq("EDIT_TRANSACTION"), eq(original.getId().toString()), argThat(s -> s.contains("Before: type=EXPENSE") && s.contains("After: type=SALARY")));
        service.edit(original.getId(), new MoneyService.EditRequest(updated, 0L), auth);
        assertEquals(1L, original.getRevision());
    }
    @Test void staleEditsAndEditsThatOverdrawDestinationAreRejected() {
        service.collection(1L, "Santosh", null, 500, 10L, auth);
        var transfer = service.record(request("TRANSFER", 1L, 2L, "500"), auth);
        service.record(request("EXPENSE", 2L, null, "400"), auth);
        assertThrows(IllegalArgumentException.class, () -> service.edit(transfer.getId(), new MoneyService.EditRequest(request("TRANSFER", 1L, 2L, "50"), 0L), auth));
        assertThrows(IllegalArgumentException.class, () -> service.edit(transfer.getId(), new MoneyService.EditRequest(request("TRANSFER", 1L, 2L, "500"), 9L), auth));
        assertEquals(0, balance(2L).compareTo(new BigDecimal("100")));
    }

    @Test void vendorAdvanceAndRecoveryMoveMoneyWithoutExpenseClassification() {
        service.collection(1L,"Santosh",null,500,10L,auth);
        var advance=service.vendorFunds("source:advance:test",1L,new BigDecimal("300"),LocalDate.now(),false,"Vendor advance",auth);
        assertEquals("VENDOR_ADVANCE",advance.getType());assertEquals(0,balance(1L).compareTo(new BigDecimal("200")));
        var recovered=service.vendorFunds("source:recovery:test",3L,new BigDecimal("100"),LocalDate.now(),true,"Refund",auth);
        assertEquals("VENDOR_RECOVERY",recovered.getType());assertEquals(0,balance(3L).compareTo(new BigDecimal("100")));
        service.reverseLinked(recovered.getId(),"Correct refund",auth);assertEquals(0,balance(3L).signum());
    }
    @Test void vendorAdvanceCannotOverdrawHeldMoneyButCanUsePersonalPocket() {
        assertThrows(IllegalArgumentException.class,()->service.vendorFunds("source:advance:held",1L,new BigDecimal("1"),LocalDate.now(),false,"Advance",auth));
        service.vendorFunds("source:advance:pocket",4L,new BigDecimal("300"),LocalDate.now(),false,"Advance",auth);
        assertEquals(0,balance(4L).compareTo(new BigDecimal("-300")));assertEquals(0,balance(1L).signum());
    }

    @Test void reimbursementRepaysPersonalSpendingWithoutAnotherExpenseAndRetriesOnce() {
        service.record(request("OPENING",null,3L,"1000"),auth);
        service.record(request("EXPENSE",4L,null,"500"),auth);
        var repayment=request("REIMBURSEMENT",3L,4L,"200");
        service.record(repayment,auth);service.record(repayment,auth);
        assertEquals(0,balance(3L).compareTo(new BigDecimal("800")));
        assertEquals(0,balance(4L).compareTo(new BigDecimal("-300")));
        assertEquals(1,journal.stream().filter(e->e.getType().equals("EXPENSE")).count());
        assertEquals(3,journal.size());
    }
    @Test void reimbursementRejectsOverpaymentWrongAccountsInsufficientFundsAndEarlyDate() {
        service.record(request("OPENING",null,3L,"100"),auth);service.record(request("EXPENSE",4L,null,"500"),auth);
        assertThrows(IllegalArgumentException.class,()->service.record(request("REIMBURSEMENT",3L,4L,"501"),auth));
        assertThrows(IllegalArgumentException.class,()->service.record(request("REIMBURSEMENT",3L,4L,"200"),auth));
        assertThrows(IllegalArgumentException.class,()->service.record(request("REIMBURSEMENT",4L,3L,"10"),auth));
        assertThrows(IllegalArgumentException.class,()->service.record(new MoneyRequest("early","REIMBURSEMENT",3L,4L,new BigDecimal("10"),"Repay",null,null,LocalDate.of(2026,11,7)),auth));
        assertEquals(2,journal.size());
    }
    @Test void reimbursementEditAdjustsDebtAndCannotRemoveAlreadyRepaidExpense() {
        service.record(request("OPENING",null,3L,"1000"),auth);var expense=service.record(request("EXPENSE",4L,null,"500"),auth);
        var repayment=service.record(request("REIMBURSEMENT",3L,4L,"200"),auth);
        service.edit(repayment.getId(),new MoneyService.EditRequest(request("REIMBURSEMENT",3L,4L,"400"),0L),auth);
        assertEquals(0,balance(3L).compareTo(new BigDecimal("600")));assertEquals(0,balance(4L).compareTo(new BigDecimal("-100")));
        assertThrows(IllegalArgumentException.class,()->service.edit(repayment.getId(),new MoneyService.EditRequest(request("REIMBURSEMENT",3L,4L,"501"),1L),auth));
        assertThrows(IllegalArgumentException.class,()->service.edit(expense.getId(),new MoneyService.EditRequest(request("EXPENSE",4L,null,"100"),0L),auth));
    }

 @Test void cashCorrectionUsesOriginalCollectorAndProtectsSpentMoney(){
  service.collection(1L,"Santosh",null,200,10L,auth);
  service.correctCollection(10L,2L,"Sandeep",null,100,LocalDate.now(),auth);
  assertEquals(0,balance(1L).compareTo(new BigDecimal("300")));assertEquals(0,balance(2L).signum());
  service.correctCollection(10L,2L,"Sandeep",null,-200,LocalDate.now(),auth);
  assertEquals(0,balance(1L).compareTo(new BigDecimal("100")));
  service.record(request("TRANSFER",1L,3L,"100"),auth);
  assertThrows(IllegalArgumentException.class,()->service.correctCollection(10L,1L,"Santosh",null,-1,LocalDate.now(),auth));
 }
}
