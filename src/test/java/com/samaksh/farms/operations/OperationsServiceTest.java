package com.samaksh.farms.operations;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.inventory.*;
import com.samaksh.farms.inventorytransaction.dto.*;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import com.samaksh.farms.inventorytransaction.service.InventoryTransactionService;
import com.samaksh.farms.production.service.ProductionBatchService;
import com.samaksh.farms.production.dto.ProductionBatchRequest;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import com.samaksh.farms.productionmovement.repo.ProductionMovementRepository;
import com.samaksh.farms.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OperationsServiceTest {
 final InventoryTransactionRepository stock=mock(InventoryTransactionRepository.class);
 final InventoryCatalogService catalog=mock(InventoryCatalogService.class);
 final AuditService audit=mock(AuditService.class);
 final Authentication auth=mock(Authentication.class);
 InventoryTransactionService inventory(){when(auth.getName()).thenReturn("admin");return new InventoryTransactionService(stock,audit,mock(com.samaksh.farms.employee.WorkAttributionService.class),catalog);}
 InventoryOption option(String category,String code,int effect){var o=new InventoryOption();o.setCode(code);o.setCategory(category);o.setActive(true);o.setEffect(effect);when(catalog.find(category,code)).thenReturn(o);return o;}
 InventoryTransactionRequest request(String type,double qty){var r=new InventoryTransactionRequest();r.setInventoryType("CUSTOM");r.setTransactionType(type);r.setQuantity(qty);return r;}
 @Test void customTypesPersistDirectionAndAudit(){option("ITEM","CUSTOM",1);option("MOVEMENT","RETURN",1);when(stock.findByInventoryType("CUSTOM")).thenReturn(List.of());var r=inventory().createTransaction(request("RETURN",5),auth);assertEquals(1,r.getStockEffect());assertEquals(5,r.getQuantity());verify(audit).createAudit(eq(auth),eq("INVENTORY"),eq("CREATE"),anyString(),anyString());}
 @Test void outgoingCannotExceedStock(){option("ITEM","CUSTOM",1);option("MOVEMENT","LOSS",-1);when(stock.findByInventoryType("CUSTOM")).thenReturn(List.of(InventoryTransaction.builder().quantity(3d).stockEffect(1).build()));assertThrows(IllegalArgumentException.class,()->inventory().createTransaction(request("LOSS",5),auth));verify(stock,never()).save(any());}
 @Test void productionMovementsMustBeEditedThroughBatch(){option("ITEM","CUSTOM",1);option("MOVEMENT","LOSS",-1);when(stock.findById(1L)).thenReturn(Optional.of(InventoryTransaction.builder().productionBatchCode("B1").build()));assertThrows(IllegalArgumentException.class,()->inventory().edit(1L,request("LOSS",1),auth));}
 @Test void reducingAnIncomingEntryCannotConsumeStockAlreadyUsed(){option("ITEM","CUSTOM",1);option("MOVEMENT","RETURN",1);var incoming=InventoryTransaction.builder().id(1L).inventoryType("CUSTOM").transactionType("RETURN").quantity(10d).stockEffect(1).build();when(stock.findById(1L)).thenReturn(Optional.of(incoming));when(stock.findByInventoryType("CUSTOM")).thenReturn(List.of(incoming,InventoryTransaction.builder().quantity(8d).stockEffect(-1).build()));assertThrows(IllegalArgumentException.class,()->inventory().edit(1L,request("RETURN",5),auth));}
 ProductionBatchRequest batchRequest(int bags){var r=new ProductionBatchRequest();r.setMushroomType(MushroomType.OYSTER);r.setBagsPrepared(bags);r.setDamagedCovers(0);r.setDamagedSpawnKg(0d);r.setDamagedPelletsKg(0d);return r;}
 ProductionBatch batch(){return ProductionBatch.builder().id(1L).batchCode("B1").mushroomType(MushroomType.OYSTER).bagsPrepared(10).spawnUsedKg(1.5).pelletsUsedKg(10d).coversUsed(10).darkRoomBags(4).lightRoomBags(6).build();}
 @Test void batchEditReturnsUnusedMaterials(){var repo=mock(ProductionBatchRepository.class);var b=batch();when(repo.findById(1L)).thenReturn(Optional.of(b));when(stock.findByInventoryType(any(InventoryType.class))).thenReturn(List.of());var svc=new ProductionBatchService(repo,stock,mock(ProductionMovementRepository.class),audit,mock(com.samaksh.farms.employee.WorkAttributionService.class),catalog);svc.updateBatch(1L,batchRequest(8),auth);assertEquals(2,b.getDarkRoomBags());assertEquals(6,b.getLightRoomBags());verify(stock,times(3)).save(argThat(t->t.getStockEffect()==1&&"B1".equals(t.getProductionBatchCode())));verify(audit).createAudit(eq(auth),eq("PRODUCTION"),eq("EDIT_BATCH"),eq("B1"),anyString());}
 @Test void cannotEraseTransferredBags(){var repo=mock(ProductionBatchRepository.class);when(repo.findById(1L)).thenReturn(Optional.of(batch()));var svc=new ProductionBatchService(repo,stock,mock(ProductionMovementRepository.class),audit,mock(com.samaksh.farms.employee.WorkAttributionService.class),catalog);assertThrows(IllegalArgumentException.class,()->svc.updateBatch(1L,batchRequest(5),auth));verify(stock,never()).save(any());}
 @Test void damageCountsTowardRequiredMaterialStock(){var repo=mock(ProductionBatchRepository.class);when(stock.findByInventoryType(InventoryType.SPAWN)).thenReturn(List.of(InventoryTransaction.builder().quantity(1.5).stockEffect(1).build()));when(stock.findByInventoryType(InventoryType.PELLET)).thenReturn(List.of(InventoryTransaction.builder().quantity(10d).stockEffect(1).build()));when(stock.findByInventoryType(InventoryType.BAG)).thenReturn(List.of(InventoryTransaction.builder().quantity(10d).stockEffect(1).build()));var r=batchRequest(10);r.setDamagedSpawnKg(.1);var svc=new ProductionBatchService(repo,stock,mock(ProductionMovementRepository.class),audit,mock(com.samaksh.farms.employee.WorkAttributionService.class),catalog);assertThrows(RuntimeException.class,()->svc.createBatch(r,auth));verify(repo,never()).save(any());}
}
