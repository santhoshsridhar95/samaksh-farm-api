package com.samaksh.farms.inventorytransaction.service;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.inventory.InventoryCatalogService;
import com.samaksh.farms.inventorytransaction.dto.*;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import com.samaksh.farms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class InventoryTransactionService {
 private final InventoryTransactionRepository repository;
 private final AuditService auditService;
    private final com.samaksh.farms.employee.WorkAttributionService attribution;
 private final InventoryCatalogService catalog;
 public InventoryTransactionResponse createTransaction(InventoryTransactionRequest r, Authentication a) { return save(null,r,a); }
 public InventoryTransactionResponse edit(Long id, InventoryTransactionRequest r, Authentication a) { return save(id,r,a); }
 private InventoryTransactionResponse save(Long id, InventoryTransactionRequest r, Authentication a) {
  catalog.lock();
  var date=r.getTransactionDate()==null?com.samaksh.farms.common.time.BusinessTime.today():r.getTransactionDate();
  if(date.isAfter(com.samaksh.farms.common.time.BusinessTime.today()))throw new IllegalArgumentException("Inventory date cannot be in the future");
  if(r.getQuantity()==null || !Double.isFinite(r.getQuantity()) || r.getQuantity()<=0) throw new IllegalArgumentException("Quantity must be greater than zero");
  var item=catalog.find("ITEM",r.getInventoryType()); var movement=catalog.find("MOVEMENT",r.getTransactionType());
  var t=id==null?new InventoryTransaction():repository.findById(id).orElseThrow(()->new IllegalArgumentException("Transaction not found"));
  if(t.getProductionBatchCode()!=null || (t.getRemarks()!=null && (t.getRemarks().startsWith("Consumed in batch ") || t.getRemarks().startsWith("Damaged during batch ")))) throw new IllegalArgumentException("Edit the production batch to update its material usage");
  if(!item.isActive() && !Objects.equals(t.getInventoryType(),r.getInventoryType()) || !movement.isActive() && !Objects.equals(t.getTransactionType(),r.getTransactionType())) throw new IllegalArgumentException("Choose an active type");
  String before=describe(t); String oldType=t.getInventoryType(); double oldDelta=t.stockDelta();
  int effect=Objects.equals(t.getTransactionType(),r.getTransactionType()) && t.getStockEffect()!=null?t.getStockEffect():movement.getEffect();
  var affected=new HashSet<String>(); affected.add(r.getInventoryType()); if(oldType!=null)affected.add(oldType);
  for(String type:affected) {
   double balance=repository.findByInventoryType(type).stream().mapToDouble(InventoryTransaction::stockDelta).sum();
   double next=balance-(Objects.equals(type,oldType)?oldDelta:0)+(type.equals(r.getInventoryType())?r.getQuantity()*effect:0);
   if(next < Math.min(0,balance)-0.000001)throw new IllegalArgumentException("Insufficient stock for "+type);
  }
  attribution.validate(r.getPerformedByEmployeeId(),t.getPerformedByEmployeeId(),date,a);
  t.setPerformedByEmployeeId(r.getPerformedByEmployeeId());
  t.setInventoryType(r.getInventoryType());t.setTransactionType(r.getTransactionType());t.setQuantity(r.getQuantity());t.setStockEffect(effect);t.setRemarks(r.getRemarks());t.setTransactionDate(r.getTransactionDate()!=null||id==null?date:(t.getTransactionDate()!=null?t.getTransactionDate():t.getCreatedAt().toLocalDate()));
  if(id==null){t.setCreatedAt(LocalDateTime.now());t.setCreatedByEmail(a.getName());if(a.getPrincipal() instanceof User u)t.setCreatedByUserId(u.getId());}
  else {t.setUpdatedAt(LocalDateTime.now());t.setUpdatedBy(a.getName());}
  repository.save(t);auditService.createAudit(a,"INVENTORY",id==null?"CREATE":"EDIT",String.valueOf(t.getId()),before+" -> "+describe(t));return map(t);
 }
 private String describe(InventoryTransaction t){return "worker="+t.getPerformedByEmployeeId()+" / "+t.getTransactionDate()+" / "+t.getInventoryType()+" / "+t.getTransactionType()+" / "+t.getQuantity()+" / "+t.getRemarks();}
 public List<InventoryTransactionResponse> getAllTransactions(){return repository.findAll().stream().map(this::map).toList();}
 private InventoryTransactionResponse map(InventoryTransaction t){return InventoryTransactionResponse.builder().id(t.getId()).performedByEmployeeId(t.getPerformedByEmployeeId()).transactionDate(t.getTransactionDate()==null?(t.getCreatedAt()==null?null:t.getCreatedAt().toLocalDate()):t.getTransactionDate()).inventoryType(t.getInventoryType()).transactionType(t.getTransactionType()).quantity(t.getQuantity()).remarks(t.getRemarks()).stockEffect(t.getStockEffect()).productionBatchCode(t.getProductionBatchCode()).createdAt(t.getCreatedAt()).createdByUserId(t.getCreatedByUserId()).createdByEmail(t.getCreatedByEmail()).build();}
}
