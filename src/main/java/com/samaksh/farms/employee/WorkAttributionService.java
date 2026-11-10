package com.samaksh.farms.employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;
import java.util.Objects;
@Service @RequiredArgsConstructor
public class WorkAttributionService {
 private final EmployeeRepository employees;
 public void validate(Long worker, Long previous, LocalDate date, Authentication auth) {
  if (!Objects.equals(worker,previous) && (auth==null || auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_SUPER_ADMIN"))))
   throw new org.springframework.security.access.AccessDeniedException("Only administrators can change work attribution");
  if(worker==null)return;
  var e=employees.findById(worker).orElseThrow(()->new IllegalArgumentException("Employee not found"));
  if(date==null || date.isBefore(e.getJoinedOn()) || (e.getLeftOn()!=null && date.isAfter(e.getLeftOn())))
   throw new IllegalArgumentException("Work date must fall within employee employment dates");
 }
}
