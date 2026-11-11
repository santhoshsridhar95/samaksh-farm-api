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
 final HarvestService service=new HarvestService(repo,batches,audit,mock(com.samaksh.farms.employee.WorkAttributionService.class));
 HarvestServiceTest(){when(repo.save(any())).thenAnswer(a->{Harvest h=a.getArgument(0);h.setId(1L);return h;});}
 HarvestRequest request(){var r=new HarvestRequest();r.setQuantity(125.5);r.setHarvestDate(BusinessTime.today().minusDays(3));return r;}
 @Test void boxesAndWorkerPersistAndEditsPreserveRecorder(){
  var r=request();r.setPerformedByEmployeeId(7L);r.setNumberOfBoxes(new java.math.BigDecimal("24"));
  var created=service.createHarvest(r,null);assertEquals(7L,created.getPerformedByEmployeeId());assertEquals(24,created.getNumberOfBoxes());
  var h=Harvest.builder().id(1L).quantity(10d).performedByEmployeeId(7L).numberOfBoxes(24).createdBy(42L).createdAt(BusinessTime.now().minusDays(1)).build();
  when(repo.findById(1L)).thenReturn(Optional.of(h));r.setPerformedByEmployeeId(8L);r.setNumberOfBoxes(java.math.BigDecimal.TEN);
  var changed=service.updateHarvest(1L,r,null);assertEquals(8L,changed.getPerformedByEmployeeId());assertEquals(10,changed.getNumberOfBoxes());assertEquals(42L,changed.getCreatedBy());
 }
 @Test void rejectsFractionalNegativeAndOverflowBoxes(){for(String value:List.of("1.5","-1","2147483648")){var r=request();r.setNumberOfBoxes(new java.math.BigDecimal(value));assertThrows(IllegalArgumentException.class,()->service.createHarvest(r,null));}verify(repo,never()).save(any());}
 @Test void combinedHarvestPersistsUserDateWithoutLookingUpBatch(){var r=request();var result=service.createHarvest(r,null);assertNull(result.getBatchId());assertNull(result.getBatchCode());assertEquals(r.getHarvestDate(),result.getHarvestDate());assertEquals(125.5,result.getQuantity());verifyNoInteractions(batches);verify(repo).save(argThat(h->h.getBatch()==null&&h.getHarvestDate().equals(r.getHarvestDate())));verify(audit).createAudit(isNull(),eq("HARVEST"),eq("CREATE_HARVEST"),eq("1"),contains(r.getHarvestDate().toString()));}
 @Test void optionalBatchRemainsSupported(){var r=request();r.setBatchId(2L);when(batches.findById(2L)).thenReturn(Optional.of(ProductionBatch.builder().id(2L).batchCode("B2").build()));var result=service.createHarvest(r,null);assertEquals("B2",result.getBatchCode());assertEquals(2L,result.getBatchId());}
 @Test void editChangesDateAndCanRemoveBatch(){var h=Harvest.builder().id(1L).batch(ProductionBatch.builder().id(2L).batchCode("B2").build()).harvestDate(BusinessTime.today()).quantity(10d).build();when(repo.findById(1L)).thenReturn(Optional.of(h));var result=service.updateHarvest(1L,request(),null);assertEquals(request().getHarvestDate(),result.getHarvestDate());assertNull(result.getBatchId());verify(audit).createAudit(isNull(),eq("HARVEST"),eq("EDIT_HARVEST"),eq("1"),contains("B2"));}
 @Test void legacyRequestDefaultsToBusinessDate(){var r=request();r.setHarvestDate(null);assertEquals(BusinessTime.today(),service.createHarvest(r,null).getHarvestDate());}
 @Test void rejectsFutureDateAndNonPositiveQuantity(){var r=request();r.setHarvestDate(BusinessTime.today().plusDays(1));assertThrows(IllegalArgumentException.class,()->service.createHarvest(r,null));r.setHarvestDate(BusinessTime.today());r.setQuantity(0d);assertThrows(IllegalArgumentException.class,()->service.createHarvest(r,null));verify(repo,never()).save(any());}
 @Test void listingUnassignedAndLegacyHarvestsIsNullSafe(){when(repo.findAll()).thenReturn(List.of(Harvest.builder().id(1L).quantity(4d).build()));assertNull(service.getAllHarvests().getFirst().getBatchId());}

 @Test void creationRecordsAuthenticatedUserAndCurrentAuditTime() {
  var user=new com.samaksh.farms.user.entity.User();user.setId(42L);
  var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user,null,List.of());
  var before=BusinessTime.now();var result=service.createHarvest(request(),auth);
  assertEquals(42L,result.getCreatedBy());assertFalse(result.getCreatedAt().isBefore(before));
  assertFalse(result.getCreatedAt().isAfter(BusinessTime.now()));
  assertEquals(request().getHarvestDate(),result.getHarvestDate());
 }
 @Test void editPreservesOriginalCreatorAndTimestamp() {
  var created=BusinessTime.now().minusDays(10);
  var h=Harvest.builder().id(1L).quantity(10d).createdBy(42L).createdAt(created).build();
  when(repo.findById(1L)).thenReturn(Optional.of(h));
  var user=new com.samaksh.farms.user.entity.User();user.setId(99L);
  var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user,null,List.of());
  var result=service.updateHarvest(1L,request(),auth);
  assertEquals(42L,result.getCreatedBy());assertEquals(created,result.getCreatedAt());
 }
 @Test void editingLegacyHarvestDoesNotInventAttribution() {
  when(repo.findById(1L)).thenReturn(Optional.of(Harvest.builder().id(1L).quantity(10d).build()));
  var result=service.updateHarvest(1L,request(),null);
  assertNull(result.getCreatedBy());assertNull(result.getCreatedAt());
 }
}
