package com.samaksh.farms.employee;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WorkAttributionServiceTest {
 final EmployeeRepository employees=mock(EmployeeRepository.class);
 final WorkAttributionService service=new WorkAttributionService(employees);
 final LocalDate date=LocalDate.of(2026,2,1);
 final UsernamePasswordAuthenticationToken admin=new UsernamePasswordAuthenticationToken("admin",null,List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
 @Test void permitsEmployeeWithoutLoginAndHistoricalWork(){var e=new Employee();e.setJoinedOn(date.minusDays(1));e.setLeftOn(date.plusDays(1));when(employees.findById(7L)).thenReturn(Optional.of(e));assertDoesNotThrow(()->service.validate(7L,null,date,admin));assertThrows(IllegalArgumentException.class,()->service.validate(7L,null,date.plusDays(2),admin));}
 @Test void rejectsNonAdminAssignmentAndClearing(){var user=new UsernamePasswordAuthenticationToken("manager",null,List.of(new SimpleGrantedAuthority("ROLE_FARM_MANAGER")));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.validate(7L,null,date,user));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.validate(null,7L,date,user));verifyNoInteractions(employees);}
 @Test void rejectsUnknownEmployee(){assertThrows(IllegalArgumentException.class,()->service.validate(123L,null,date,admin));}
 @Test void leavesLegacyWorkUnassigned(){assertDoesNotThrow(()->service.validate(null,null,date,null));verifyNoInteractions(employees);}
}
