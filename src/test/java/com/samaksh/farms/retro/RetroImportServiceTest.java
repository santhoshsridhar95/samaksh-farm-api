package com.samaksh.farms.retro;
import com.samaksh.farms.finance.*;
import com.samaksh.farms.sourcing.*;
import com.samaksh.farms.harvest.service.HarvestService;
import com.samaksh.farms.harvest.dto.HarvestResponse;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import jakarta.persistence.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class RetroImportServiceTest {
 final RetroImportLogRepository logs=mock(RetroImportLogRepository.class);
 final HarvestService harvest=mock(HarvestService.class);
 final Validator validator=mock(Validator.class);
 final EntityManager em=mock(EntityManager.class);
 final AuditService audit=mock(AuditService.class);
 final Authentication auth=mock(Authentication.class);
 final RetroImportService service=new RetroImportService(null,null,null,null,harvest,null,null,logs,validator,em,audit,null,null,mock(ProductionBatchRepository.class),null,null,null);
 RetroImportServiceTest(){when(validator.validate(any())).thenReturn(Set.of());when(auth.getName()).thenReturn("admin");var query=mock(Query.class);when(em.createNativeQuery(anyString())).thenReturn(query);when(harvest.createHarvest(any(),any())).thenReturn(HarvestResponse.builder().id(42L).build());when(logs.save(any())).thenAnswer(a->{RetroImportLog l=a.getArgument(0);when(logs.findByModuleAndExternalReferenceIn(eq(l.getModule()),any())).thenReturn(List.of(l));return l;});}
 Map<String,String> row(){return Map.of("externalReference","harvest-old-1","quantity","12.5","harvestDate","2024-01-01");}
 @Test void importsHistoricalDateOnceAndAuditsActualImport(){assertEquals("IMPORTED",service.commit("HARVEST",List.of(row()),auth).getFirst().status());verify(harvest).createHarvest(argThat(r->r.getHarvestDate().toString().equals("2024-01-01")&&r.getBatchId()==null),eq(auth));assertEquals("SKIP",service.commit("HARVEST",List.of(row()),auth).getFirst().status());verify(harvest,times(1)).createHarvest(any(),any());verify(audit).createAudit(eq(auth),eq("RETRO_IMPORT"),eq("IMPORT_HARVEST"),eq("42"),anyString());}
 @Test void changedImportedReferenceIsRejected(){service.commit("HARVEST",List.of(row()),auth);var changed=new HashMap<>(row());changed.put("quantity","15");assertEquals("ERROR",service.preview("HARVEST",List.of(changed)).getFirst().status());}
 @Test void validatesAllRowsBeforeAnyWrite(){var invalid=new HashMap<>(row());invalid.put("externalReference","second");invalid.put("batchId","999");assertThrows(IllegalArgumentException.class,()->service.commit("HARVEST",List.of(row(),invalid),auth));verifyNoInteractions(harvest);}
 @Test void duplicateReferencesAndUnknownColumnsAreReported(){var results=service.preview("HARVEST",List.of(row(),row()));assertEquals("ERROR",results.get(1).status());var invalid=new HashMap<>(row());invalid.put("unexpected","value");assertEquals("ERROR",service.preview("HARVEST",List.of(invalid)).getFirst().status());}
}
