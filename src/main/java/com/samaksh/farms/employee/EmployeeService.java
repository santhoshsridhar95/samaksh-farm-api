package com.samaksh.farms.employee;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.finance.*;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.user.repo.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class EmployeeService {
 private final EmployeeRepository employees;
 private final EmployeeSalaryRepository salaries;
 private final EmployeeWorkRepository work;
 private final MoneyEntryRepository money;
 private final SaleRepository sales;
 private final UserRepository users;
 private final AuditService audit;
 private final EntityManager em;

 public record ProfileRequest(@NotBlank @Size(max=80) String requestKey,
   @NotBlank @Size(max=150) String name,@NotBlank @Size(max=150) String designation,
   @Size(max=300) String responsibilities,Long userId,@NotNull @PastOrPresent LocalDate joinedOn,
   @PastOrPresent LocalDate leftOn,boolean active,Long version,
   @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal salary,@NotBlank String effectiveMonth) {}
 public record WorkRequest(@NotBlank @Size(max=80) String requestKey,@NotNull @PastOrPresent LocalDate date,
   @NotBlank String category,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal quantity,
   @NotBlank @Size(max=1000) String notes) {}

 @Transactional(readOnly=true)
 public Map<String,Object> list(){return Map.of("employees",employees.findAllByOrderByNameAsc(),"salaries",salaries.findAll());}
 private Employee get(Long id){return employees.findById(id).orElseThrow(()->new IllegalArgumentException("Employee not found"));}
 private void lock(){em.createNativeQuery("SELECT pg_advisory_xact_lock(734215909)").getSingleResult();}
 public Employee save(Long id,ProfileRequest r,Authentication a){
   lock();
   YearMonth month=parseMonth(r.effectiveMonth());
   if(r.leftOn()!=null&&r.leftOn().isBefore(r.joinedOn()))throw new IllegalArgumentException("Leaving date must be on or after joining date");
   if(r.active()&&r.leftOn()!=null)throw new IllegalArgumentException("An active employee cannot have a leaving date");
   if(month.isBefore(YearMonth.from(r.joinedOn())))throw new IllegalArgumentException("Salary cannot start before the joining month");
   if(id==null){var previous=employees.findByRequestKey(r.requestKey());if(previous.isPresent()){if(!MoneyService.fingerprint(r).equals(previous.get().getCreateFingerprint()))throw new IllegalArgumentException("Request already saved with different employee details");return previous.get();}}
   Employee e=id==null?new Employee():get(id);
   if(id!=null&&!Objects.equals(e.getVersion(),r.version()))throw new IllegalArgumentException("Employee changed. Refresh before editing.");
   if(r.userId()!=null){
     if(!users.existsById(r.userId()))throw new IllegalArgumentException("Login user not found");
     if(employees.existsByUserIdAndIdNot(r.userId(),id==null?-1L:id))throw new IllegalArgumentException("This login is already linked to another employee");
   }
   String before=details(e);
   if(id==null){e.setCreateFingerprint(MoneyService.fingerprint(r));e.setRequestKey(r.requestKey());e.setRecordedAt(BusinessTime.now());e.setRecordedBy(a.getName());}
   e.setName(r.name().trim());e.setDesignation(r.designation().trim());e.setResponsibilities(r.responsibilities());e.setUserId(r.userId());
   e.setJoinedOn(r.joinedOn());e.setLeftOn(r.leftOn());e.setActive(r.active());e.setUpdatedAt(BusinessTime.now());e.setUpdatedBy(a.getName());
   employees.saveAndFlush(e);
   var salary=salaries.findByEmployeeIdAndEffectiveMonth(e.getId(),month.toString()).orElseGet(EmployeeSalary::new);
   var oldSalary=salary.getAmount();
   salary.setEmployeeId(e.getId());salary.setEffectiveMonth(month.toString());salary.setAmount(r.salary());salary.setUpdatedAt(BusinessTime.now());salary.setUpdatedBy(a.getName());salaries.save(salary);
   audit.createAudit(a,"EMPLOYEE",id==null?"CREATE":"EDIT",e.getId().toString(),"Before: "+before+" | After: "+details(e)+" | Salary from "+month+": "+oldSalary+" -> "+r.salary());
   return e;
 }
 private String details(Employee e){return "name="+e.getName()+", designation="+e.getDesignation()+", responsibilities="+e.getResponsibilities()+", login="+e.getUserId()+", joined="+e.getJoinedOn()+", left="+e.getLeftOn()+", active="+e.isActive();}
 static YearMonth parseMonth(String value){try{return YearMonth.parse(value);}catch(Exception ex){throw new IllegalArgumentException("Use a valid salary month (YYYY-MM)");}}
 public EmployeeWork work(Long id,WorkRequest r,Authentication a){
   lock();var employee=get(id);
   var existing=work.findByRequestKey(r.requestKey());
   String fingerprint=MoneyService.fingerprint(id+":"+r);
   if(existing.isPresent()){if(!fingerprint.equals(existing.get().getFingerprint()))throw new IllegalArgumentException("Request already saved with different details");return existing.get();}
   if(!Set.of("BAGGING","HARVEST_KG","DELIVERY_BOXES","SHOP_SETUP","HOURS","OTHER").contains(r.category()))throw new IllegalArgumentException("Invalid contribution category");
   if(r.date().isBefore(employee.getJoinedOn())||(employee.getLeftOn()!=null&&r.date().isAfter(employee.getLeftOn())))throw new IllegalArgumentException("Work date must fall within employment dates");
   if(Set.of("BAGGING","SHOP_SETUP").contains(r.category())&&r.quantity().stripTrailingZeros().scale()>0)throw new IllegalArgumentException("Bags and shops must be whole numbers");
   var w=new EmployeeWork();w.setRequestKey(r.requestKey());w.setFingerprint(fingerprint);w.setEmployeeId(id);w.setDate(r.date());w.setCategory(r.category());w.setQuantity(r.quantity());w.setNotes(r.notes().trim());w.setRecordedAt(BusinessTime.now());w.setRecordedBy(a.getName());work.save(w);
   audit.createAudit(a,"EMPLOYEE_WORK","CREATE",w.getId().toString(),"Employee #"+id+": "+r);
   return w;
 }
 public EmployeeWork voidWork(Long id,Long workId,Authentication a){
   lock();var w=work.findById(workId).orElseThrow(()->new IllegalArgumentException("Contribution not found"));
   if(!w.getEmployeeId().equals(id))throw new IllegalArgumentException("Contribution belongs to another employee");
   if(!w.isVoided()){w.setVoided(true);audit.createAudit(a,"EMPLOYEE_WORK","VOID",workId.toString(),"Employee #"+id+", date="+w.getDate()+", category="+w.getCategory()+", quantity="+w.getQuantity()+", notes="+w.getNotes());}
   return w;
 }
 public record Activity(String reference,LocalDate date,String category,BigDecimal quantity,String unit,String details,BigDecimal revenue,BigDecimal exchangeBoxes,BigDecimal returnedBoxes){}
 public record Payroll(String month,BigDecimal salary,BigDecimal paid,BigDecimal remaining,BigDecimal overpaid){}
 @Transactional(readOnly=true)
 public Map<String,Object> report(Long id,LocalDate from,LocalDate to){
   if(from==null||to==null||to.isBefore(from)||to.isAfter(BusinessTime.today())||to.isAfter(from.plusDays(366)))throw new IllegalArgumentException("Choose a date range of up to one year ending today or earlier");
   var e=get(id);var history=salaries.findByEmployeeIdOrderByEffectiveMonthDesc(id);
   var contributions=work.findByEmployeeIdAndDateBetweenOrderByDateDescIdDesc(id,from,to);
   var payments=money.findByEmployeeIdAndDateBetweenOrderByDateDescIdDesc(id,from,to);
   var salaryPayments=money.findByEmployeeIdAndSalaryMonthBetween(id,YearMonth.from(from).toString(),YearMonth.from(to).toString());
   var payroll=new ArrayList<Payroll>();
   for(var month=YearMonth.from(from);!month.isAfter(YearMonth.from(to));month=month.plusMonths(1)){
     final String key=month.toString();
     BigDecimal rate=salaryFor(history,e,month);
     BigDecimal paid=salaryPayments.stream().filter(p->key.equals(p.getSalaryMonth())).map(p->"SALARY".equals(p.getType())?p.getAmount():"REVERSAL".equals(p.getType())?p.getAmount().negate():BigDecimal.ZERO).reduce(BigDecimal.ZERO,BigDecimal::add);
     payroll.add(new Payroll(key,rate,paid,rate.subtract(paid).max(BigDecimal.ZERO),paid.subtract(rate).max(BigDecimal.ZERO)));
   }
   var activity=new ArrayList<Activity>();
   if(e.getUserId()!=null){
     var rows=sales.findByCreatedByUserIdAndSaleDateGreaterThanEqualAndSaleDateLessThan(e.getUserId(),from.atStartOfDay(),to.plusDays(1).atStartOfDay());
     for(var s:rows)activity.add(new Activity("Sale #"+s.getId(),s.getSaleDate().toLocalDate(),"SALE",decimal(s.getQuantity()),s.getProduct()==null||s.getProduct().getUnitType()==null?"units":s.getProduct().getUnitType().name(),s.getCustomer()==null?"Direct sale":s.getCustomer().getCustomerName(),decimal(s.getTotalAmount()),decimal(s.getExchangeBoxes()),decimal(s.getReturnedBoxes())));
   }
   return Map.of("employee",e,"salaryHistory",history,"payroll",payroll,"payments",payments,"contributions",contributions,"activity",activity);
 }
 static BigDecimal salaryFor(List<EmployeeSalary> history,Employee e,YearMonth month){
   if(month.atEndOfMonth().isBefore(e.getJoinedOn())||(e.getLeftOn()!=null&&month.atDay(1).isAfter(e.getLeftOn())))return BigDecimal.ZERO;
   return history.stream().filter(s->s.getEffectiveMonth().compareTo(month.toString())<=0).max(Comparator.comparing(EmployeeSalary::getEffectiveMonth)).map(EmployeeSalary::getAmount).orElse(BigDecimal.ZERO);
 }
 private static BigDecimal decimal(Double n){return n==null?BigDecimal.ZERO:BigDecimal.valueOf(n);}
}
