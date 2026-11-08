package com.samaksh.farms.harvest;
import com.samaksh.farms.harvest.service.HarvestService;
import com.samaksh.farms.harvest.dto.HarvestRequest;
import com.samaksh.farms.harvest.entity.Harvest;
import com.samaksh.farms.harvest.repo.HarvestRepository;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.time.BusinessTime;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class HarvestServiceTest {
 final HarvestRepository repo=mock(HarvestRepository.class);
 final ProductionBatchRepository batches=mock(ProductionBatchRepository.class);
 final AuditService audit=mock(AuditService.class);
 final HarvestService service=new HarvestService(repo,batches,audit);
 HarvestServiceTest(){when(repo.save(any())).thenAnswer(a->{Harvest h=a.getArgument(0);h.setId(1L);return h;});}
 HarvestRequest request(){var r=new HarvestRequest();r.setQuantity(125.5);r.setHarvestDate(BusinessTime.today().minusDays(3));return r;}
 @Test void combinedHarvestPersistsUserDateWithoutLookingUpBatch(){var r=request();var result=service.createHarvest(r,null);assertNull(result.getBatchId());assertNull(result.getBatchCode());assertEquals(r.getHarvestDate(),result.getHarvestDate());assertEquals(125.5,result.getQuantity());verifyNoInteractions(batches);verify(repo).save(argThat(h->h.getBatch()==null&&h.getHarvestDate().equals(r.getHarvestDate())));verify(audit).createAudit(isNull(),eq("HARVEST"),eq("CREATE_HARVEST"),eq("1"),contains(r.getHarvestDate().toString()));}
 @Test void optionalBatchRemainsSupported(){var r=request();r.setBatchId(2L);when(batches.findById(2L)).thenReturn(Optional.of(ProductionBatch.builder().id(2L).batchCode("B2").build()));var result=service.createHarvest(r,null);assertEquals("B2",result.getBatchCode());assertEquals(2L,result.getBatchId());}
 @Test void editChangesDateAndCanRemoveBatch(){var h=Harvest.builder().id(1L).batch(ProductionBatch.builder().id(2L).batchCode("B2").build()).harvestDate(BusinessTime.today()).quantity(10d).build();when(repo.findById(1L)).thenReturn(Optional.of(h));var result=service.updateHarvest(1L,request(),null);assertEquals(request().getHarvestDate(),result.getHarvestDate());assertNull(result.getBatchId());verify(audit).createAudit(isNull(),eq("HARVEST"),eq("EDIT_HARVEST"),eq("1"),contains("B2"));}
 @Test void legacyRequestDefaultsToBusinessDate(){var r=request();r.setHarvestDate(null);assertEquals(BusinessTime.today(),service.createHarvest(r,null).getHarvestDate());}
 @Test void rejectsFutureDateAndNonPositiveQuantity(){var r=request();r.setHarvestDate(BusinessTime.today().plusDays(1));assertThrows(IllegalArgumentException.class,()->service.createHarvest(r,null));r.setHarvestDate(BusinessTime.today());r.setQuantity(0d);assertThrows(IllegalArgumentException.class,()->service.createHarvest(r,null));verify(repo,never()).save(any());}
 @Test void listingUnassignedAndLegacyHarvestsIsNullSafe(){when(repo.findAll()).thenReturn(List.of(Harvest.builder().id(1L).quantity(4d).build()));assertNull(service.getAllHarvests().getFirst().getBatchId());}
}
