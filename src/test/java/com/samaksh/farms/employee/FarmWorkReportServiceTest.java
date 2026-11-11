package com.samaksh.farms.employee;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import com.samaksh.farms.harvest.entity.Harvest;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.productionmovement.entity.ProductionMovement;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class FarmWorkReportServiceTest {
 final EntityManager em=mock(EntityManager.class);
 final LocalDate date=LocalDate.of(2026,2,1);
 <T> TypedQuery<T> query(Class<T> type,List<T> rows){
  TypedQuery<T> q=mock(TypedQuery.class,RETURNS_SELF);
  when(em.createQuery(contains("performedByEmployeeId=:employee"),eq(type))).thenReturn(q);
  when(q.getResultList()).thenReturn(rows);return q;
 }
 @Test void reportUsesAssignedEmployeeAndSourceQuantitiesWithoutLogin(){
  var h=Harvest.builder().id(1L).harvestDate(date).quantity(12.5).numberOfBoxes(50).createdBy(99L).performedByEmployeeId(7L).build();
  var harvest=query(Harvest.class,List.of(h));
  var batch=query(ProductionBatch.class,List.of(ProductionBatch.builder().batchCode("B1").startDate(date).bagsPrepared(100).build()));
  var inventory=query(InventoryTransaction.class,List.of(InventoryTransaction.builder().id(2L).transactionDate(date).quantity(20d).inventoryType("SPAWN").transactionType("PURCHASE").build()));
  var movement=query(ProductionMovement.class,List.of(ProductionMovement.builder().id(3L).movementDate(date.atStartOfDay()).movedToLightRoom(80).contaminatedBags(2).discardedBags(1).build()));
  var service=new FarmWorkReportService(em);var rows=service.report(7L,date,date);
  assertEquals(4,rows.size());var output=rows.stream().filter(r->r.category().equals("HARVEST_KG")).findFirst().orElseThrow();assertEquals(12.5,output.quantity());assertEquals(50,output.numberOfBoxes());
  for(var q:List.of(harvest,batch,inventory,movement))verify(q).setParameter("employee",7L);
  verify(harvest).setParameter("from",date);verify(harvest).setParameter("to",date);verify(movement).setParameter("to",date.plusDays(1).atStartOfDay());
  h.setQuantity(10d);h.setNumberOfBoxes(40);rows=service.report(7L,date,date);assertEquals(4,rows.size());assertEquals(40,rows.stream().filter(r->r.category().equals("HARVEST_KG")).findFirst().orElseThrow().numberOfBoxes());
 }
}
