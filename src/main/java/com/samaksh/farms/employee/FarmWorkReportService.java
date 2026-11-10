package com.samaksh.farms.employee;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import com.samaksh.farms.harvest.entity.Harvest;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.productionmovement.entity.ProductionMovement;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
@Service @RequiredArgsConstructor
public class FarmWorkReportService {
 private final EntityManager em;
 public record FarmWork(String reference,LocalDate date,String category,double quantity,String unit,Integer numberOfBoxes,String details) {}
 @Transactional(readOnly=true)
 public List<FarmWork> report(Long employeeId,LocalDate from,LocalDate to) {
  var result=new ArrayList<FarmWork>();
  for(var h:rows(Harvest.class,"harvestDate",employeeId,from,to))result.add(new FarmWork("Harvest #"+h.getId(),h.getHarvestDate(),"HARVEST_KG",h.getQuantity(),"kg",h.getNumberOfBoxes(),h.getRemarks()));
  for(var b:rows(ProductionBatch.class,"startDate",employeeId,from,to))result.add(new FarmWork("Batch "+b.getBatchCode(),b.getStartDate(),"BAGGING",b.getBagsPrepared(),"bags",null,b.getRemarks()));
  for(var t:rows(InventoryTransaction.class,"transactionDate",employeeId,from,to))result.add(new FarmWork("Inventory #"+t.getId(),t.getTransactionDate(),"INVENTORY",t.getQuantity(),t.getInventoryType(),null,t.getTransactionType()+" · "+Objects.toString(t.getRemarks(),"")));
  var movements=em.createQuery("select m from ProductionMovement m where m.performedByEmployeeId=:employee and m.movementDate>=:from and m.movementDate<:to",ProductionMovement.class).setParameter("employee",employeeId).setParameter("from",from.atStartOfDay()).setParameter("to",to.plusDays(1).atStartOfDay()).getResultList();
  for(var m:movements)result.add(new FarmWork("Room transfer #"+m.getId(),m.getMovementDate().toLocalDate(),"ROOM_TRANSFER",m.getMovedToLightRoom(),"bags moved",null,"Contaminated: "+m.getContaminatedBags()+", discarded: "+m.getDiscardedBags()+" · "+Objects.toString(m.getRemarks(),"")));
  result.sort(Comparator.comparing(FarmWork::date).reversed().thenComparing(FarmWork::reference));return result;
 }
 private <T> List<T> rows(Class<T> type,String date,Long employee,LocalDate from,LocalDate to) {
  return em.createQuery("select r from "+type.getSimpleName()+" r where r.performedByEmployeeId=:employee and r."+date+" between :from and :to",type).setParameter("employee",employee).setParameter("from",from).setParameter("to",to).getResultList();
 }
}
