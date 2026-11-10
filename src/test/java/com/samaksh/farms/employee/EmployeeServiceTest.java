package com.samaksh.farms.employee;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.finance.*;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.user.repo.UserRepository;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.security.core.Authentication;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {
 EmployeeRepository employees=mock(EmployeeRepository.class);
 EmployeeSalaryRepository salaries=mock(EmployeeSalaryRepository.class);
 EmployeeWorkRepository work=mock(EmployeeWorkRepository.class);
 MoneyEntryRepository money=mock(MoneyEntryRepository.class);
 SaleRepository sales=mock(SaleRepository.class);
 UserRepository users=mock(UserRepository.class);
 AuditService audit=mock(AuditService.class);
 EntityManager em=mock(EntityManager.class);
 Authentication auth=mock(Authentication.class);
 EmployeeService service=new EmployeeService(employees,salaries,work,money,sales,users,audit,em);
 Employee employee;
 @BeforeEach void setup(){
  employee=new Employee();employee.setId(1L);employee.setJoinedOn(LocalDate.of(2026,1,1));employee.setVersion(0L);
  when(employees.findById(1L)).thenReturn(Optional.of(employee));
  when(em.createNativeQuery(anyString())).thenReturn(mock(Query.class));when(auth.getName()).thenReturn("Admin");
 }
 EmployeeSalary rate(String month,String amount){var s=new EmployeeSalary();s.setEmployeeId(1L);s.setEffectiveMonth(month);s.setAmount(new BigDecimal(amount));return s;}
 @Test void effectiveSalaryDoesNotRewriteEarlierMonthsAndStopsAfterLeaving(){
  var rates=List.of(rate("2026-01","10000"),rate("2026-06","12000"));
  assertEquals(new BigDecimal("10000"),EmployeeService.salaryFor(rates,employee,YearMonth.of(2026,5)));
  assertEquals(new BigDecimal("12000"),EmployeeService.salaryFor(rates,employee,YearMonth.of(2026,6)));
  employee.setLeftOn(LocalDate.of(2026,7,15));
  assertEquals(BigDecimal.ZERO,EmployeeService.salaryFor(rates,employee,YearMonth.of(2026,8)));
 }
 @Test void payrollUsesSalaryMonthAndReversalsInsteadOfPaymentDate(){
  when(salaries.findByEmployeeIdOrderByEffectiveMonthDesc(1L)).thenReturn(List.of(rate("2026-01","10000")));
  var paid=new MoneyEntry();paid.setType("SALARY");paid.setSalaryMonth("2026-02");paid.setAmount(new BigDecimal("6000"));paid.setDate(LocalDate.of(2026,3,1));
  var reversed=new MoneyEntry();reversed.setType("REVERSAL");reversed.setSalaryMonth("2026-02");reversed.setAmount(new BigDecimal("1000"));
  when(money.findByEmployeeIdAndSalaryMonthBetween(1L,"2026-02","2026-02")).thenReturn(List.of(paid,reversed));
  var report=service.report(1L,LocalDate.of(2026,2,1),LocalDate.of(2026,2,28));
  var payroll=(List<EmployeeService.Payroll>)report.get("payroll");
  assertEquals(new BigDecimal("5000"),payroll.getFirst().paid());assertEquals(new BigDecimal("5000"),payroll.getFirst().remaining());
 }
 @Test void contributionsRejectFractionalBagsAndDatesOutsideEmployment(){
  assertThrows(IllegalArgumentException.class,()->service.work(1L,new EmployeeService.WorkRequest("a",LocalDate.of(2026,2,1),"BAGGING",new BigDecimal("1.5"),"Batch 1"),auth));
  assertThrows(IllegalArgumentException.class,()->service.work(1L,new EmployeeService.WorkRequest("b",LocalDate.of(2025,2,1),"HARVEST_KG",BigDecimal.ONE,"Harvest 1"),auth));
  verify(work,never()).save(any());
 }
 @Test void staleProfileCannotOverwriteSalary(){
  var request=new EmployeeService.ProfileRequest("key","Name","Founder","Farm, sales",null,employee.getJoinedOn(),null,true,3L,BigDecimal.TEN,"2026-02");
  assertThrows(IllegalArgumentException.class,()->service.save(1L,request,auth));verify(salaries,never()).save(any());
 }
 @Test void reportRejectsUnboundedRange(){assertThrows(IllegalArgumentException.class,()->service.report(1L,LocalDate.of(2020,1,1),LocalDate.of(2026,1,1)));}
 @Test void cannotVoidAnotherEmployeesWork(){var w=new EmployeeWork();w.setEmployeeId(2L);when(work.findById(4L)).thenReturn(Optional.of(w));assertThrows(IllegalArgumentException.class,()->service.voidWork(1L,4L,auth));assertFalse(w.isVoided());}
}
