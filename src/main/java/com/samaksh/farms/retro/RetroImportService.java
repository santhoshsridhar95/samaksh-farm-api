package com.samaksh.farms.retro;
import com.samaksh.farms.finance.*;
import com.samaksh.farms.sourcing.*;
import com.samaksh.farms.inventory.*;
import com.samaksh.farms.inventorytransaction.dto.InventoryTransactionRequest;
import com.samaksh.farms.inventorytransaction.service.InventoryTransactionService;
import com.samaksh.farms.production.dto.*;
import com.samaksh.farms.production.service.ProductionBatchService;
import com.samaksh.farms.harvest.dto.HarvestRequest;
import com.samaksh.farms.harvest.service.HarvestService;
import com.samaksh.farms.order.dto.CustomerOrderRequest;
import com.samaksh.farms.order.service.CustomerOrderService;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.time.BusinessTime;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.lang.reflect.*;
@Service @RequiredArgsConstructor @Transactional
public class RetroImportService {
 private final MoneyService money;
 private final InventoryTransactionService inventory;
 private final InventoryCatalogService catalog;
 private final ProductionBatchService production;
 private final HarvestService harvest;
 private final CustomerOrderService orders;
 private final SourcingService sourcing;
 private final RetroImportLogRepository logs;
 private final Validator validator;
 private final EntityManager em;
 private final AuditService audit;
 private final com.samaksh.farms.customer.repo.CustomerRepository customers;
 private final com.samaksh.farms.products.repo.ProductRepository products;
 private final com.samaksh.farms.production.repo.ProductionBatchRepository batches;
 private final SourcingVendorRepository vendors;
 private final SourcingReceiptRepository receipts;
 private final SourcingPaymentRepository payments;
 public record AccountRequest(String name,String kind){}
 record Spec(String label,Class<?> type,String required,String help){}
 private static final Map<String,Spec> SPECS=new LinkedHashMap<>();
 static {
  SPECS.put("MONEY",new Spec("Money & expenses",MoneyRequest.class,"type,amount,date","Expenses, salary, transfers, opening balances and contributions. Import opening balances before spending. Do not re-import money already entered."));
  SPECS.put("MONEY_ACCOUNT",new Spec("Money accounts",AccountRequest.class,"name,kind","Create accounts before importing their transactions."));
  SPECS.put("INVENTORY_TYPE",new Spec("Inventory / transaction types",InventoryCatalogService.OptionRequest.class,"category,code,label","Use ITEM with a unit, or MOVEMENT with effect 1 for stock in and -1 for stock out."));
  SPECS.put("INVENTORY",new Spec("Inventory movements",InventoryTransactionRequest.class,"inventoryType,transactionType,quantity,transactionDate","Quantity must be positive; the transaction type determines the stock direction. Import purchases before production."));
  SPECS.put("PRODUCTION",new Spec("Production batches",ProductionBatchRequest.class,"mushroomType,bagsPrepared,startDate","Creates batches and consumes material stock, including damage. Import inventory first. Use resulting batch IDs for room transfers."));
  SPECS.put("ROOM_TRANSFER",new Spec("Room transfers",RoomTransferRequest.class,"batchId,movementDate","Moves bags from dark to light room and records losses. Cannot exceed remaining dark-room bags."));
  SPECS.put("HARVEST",new Spec("Harvest",HarvestRequest.class,"quantity,harvestDate","Batch is optional for combined harvests."));
  SPECS.put("ORDER",new Spec("Orders",CustomerOrderRequest.class,"customerId,productId,quantity,expectedUnitPrice,orderDate","Historical order status is recorded only; importing a fulfilled order does not create a duplicate sale."));
  SPECS.put("VENDOR",new Spec("Vendors / own-farm sources",SourcingRequests.Vendor.class,"name,kind,boxesPerKg,costPerKg,expensesBorneBy","Onboard sources first, then use their IDs for receipts. Existing source prices are not overwritten."));
  SPECS.put("RECEIPT",new Spec("Sourcing receipts",SourcingRequests.Receipt.class,"vendorId,date,product,unit,quantity,costPerKg,expensesBorneBy,expensesPaidBy","Import receipts before vendor payments. Transport paid by Samaksh also debits the selected money account. Set the common selling price first."));
  SPECS.put("PAYMENT",new Spec("Vendor payments",SourcingRequests.Payment.class,"receiptId,sourceAccountId,amount,date","Invoice payment and optional recoverable additional amount debit the chosen account."));
  SPECS.put("RECOVERY",new Spec("Vendor advance recoveries",SourcingRequests.Recovery.class,"paymentId,destinationAccountId,amount,date","Records money actually recovered; cannot exceed the unpaid advance."));
 }
 public List<Map<String,Object>> modules(){return SPECS.entrySet().stream().map(e->Map.<String,Object>of("id",e.getKey(),"label",e.getValue().label(),"help",e.getValue().help())).toList();}
 private Spec spec(String module){var s=SPECS.get(module);if(s==null)throw new IllegalArgumentException("Unknown import module");return s;}
 private Map<String,Class<?>> fields(String module){var s=spec(module);var result=new LinkedHashMap<String,Class<?>>();result.put("externalReference",String.class);if(module.equals("ROOM_TRANSFER"))result.put("batchId",Long.class);for(var f:s.type().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&!f.getName().equals("requestKey"))result.put(f.getName(),f.getType());return result;}
 private String defaultValue(String name,Class<?> type){if(name.equals("active"))return "true";if(name.equals("type"))return "EXPENSE";if(name.equals("kind"))return "VENDOR";if(name.equals("category"))return "ITEM";if(name.equals("product"))return "Mushrooms";if(name.equals("unit"))return "BOX";if(name.equals("expensesBorneBy")||name.equals("expensesPaidBy"))return "VENDOR";if(name.equals("status"))return "PENDING";if(name.startsWith("damaged")||Set.of("transportCost","otherCost","soldKg","additionalAmount","movedToLightRoom","contaminatedBags","discardedBags").contains(name))return "0";return "";}
 private List<String> choices(String module,String name,Class<?> type){if(type.isEnum())return Arrays.stream(type.getEnumConstants()).map(Object::toString).toList();if(type==boolean.class)return List.of("true","false");if(name.equals("type"))return List.of("EXPENSE","SALARY","TRANSFER","REIMBURSEMENT","OPENING","CONTRIBUTION");if(name.equals("kind"))return module.equals("MONEY_ACCOUNT")?List.of("HELD","PERSONAL","BANK","SAVINGS"):List.of("VENDOR","OWN_FARM");if(name.equals("category"))return List.of("ITEM","MOVEMENT");if(name.equals("expensesBorneBy")||name.equals("expensesPaidBy"))return List.of("VENDOR","FARM");if(module.equals("RECEIPT")&&name.equals("unit"))return List.of("BOX","KG");return List.of();}
 public Map<String,Object> schema(String module){var required=new HashSet<>(Arrays.asList(spec(module).required().split(",")));required.add("externalReference");var columns=fields(module).entrySet().stream().map(e->{var t=e.getValue();return Map.<String,Object>of("name",e.getKey(),"required",required.contains(e.getKey()),"type",t==LocalDate.class?"date":Number.class.isAssignableFrom(t)?"number":"text","choices",choices(module,e.getKey(),t),"defaultValue",module.equals("MONEY_ACCOUNT")&&e.getKey().equals("kind")?"HELD":defaultValue(e.getKey(),t));}).toList();return Map.of("fields",columns,"lookups",lookups(module),"help",spec(module).help());}
 private Map<String,List<Map<String,Object>>> lookups(String module){var map=new HashMap<String,List<Map<String,Object>>>();var names=fields(module).keySet();if(names.stream().anyMatch(n->Set.of("sourceId","destinationId","sourceAccountId","destinationAccountId","expenseAccountId").contains(n))){var choices=money.availableAccounts().stream().map(a->Map.<String,Object>of("value",a.getId(),"label",a.getName()+" ("+a.getKind()+")")).toList();for(String n:List.of("sourceId","destinationId","sourceAccountId","destinationAccountId","expenseAccountId"))if(names.contains(n))map.put(n,choices);}
  if(names.contains("batchId"))map.put("batchId",batches.findAll().stream().map(b->Map.<String,Object>of("value",b.getId(),"label",b.getBatchCode())).toList());
  if(names.contains("customerId"))map.put("customerId",customers.findAll().stream().map(c->Map.<String,Object>of("value",c.getId(),"label",c.getCustomerName())).toList());
  if(names.contains("productId"))map.put("productId",products.findAll().stream().map(p->Map.<String,Object>of("value",p.getId(),"label",p.getProductName())).toList());
  if(names.contains("vendorId"))map.put("vendorId",vendors.findAll().stream().map(v->Map.<String,Object>of("value",v.getId(),"label",v.getName())).toList());
  if(names.contains("receiptId"))map.put("receiptId",receipts.findAll().stream().map(r->Map.<String,Object>of("value",r.getId(),"label","Receipt #"+r.getId()+" · "+r.getDate()+" · source #"+r.getVendorId())).toList());
  if(names.contains("paymentId"))map.put("paymentId",payments.findAll().stream().map(p->Map.<String,Object>of("value",p.getId(),"label","Payment #"+p.getId()+" · "+p.getDate())).toList());
  if(module.equals("INVENTORY")){var all=catalog.list();for(String category:List.of("ITEM","MOVEMENT"))map.put(category.equals("ITEM")?"inventoryType":"transactionType",all.stream().filter(o->o.getCategory().equals(category)&&o.isActive()).map(o->Map.<String,Object>of("value",o.getCode(),"label",o.getLabel())).toList());}
  return map;
 }
 public record RowResult(int row,String reference,String status,String message,String targetId){}
 public List<RowResult> preview(String module,List<Map<String,String>> rows){if(rows.isEmpty()||rows.size()>200)throw new IllegalArgumentException("Provide 1 to 200 rows");var refs=lookups(module);var imported=new HashMap<String,RetroImportLog>();logs.findByModuleAndExternalReferenceIn(module,rows.stream().map(r->r.getOrDefault("externalReference","").trim()).toList()).forEach(l->imported.put(l.getExternalReference(),l));var seen=new HashSet<String>();var result=new ArrayList<RowResult>();int i=1;for(var row:rows){String ref=row.getOrDefault("externalReference","").trim();try{validateRow(module,row,refs);if(!seen.add(ref))throw new IllegalArgumentException("Repeated reference in this file");var prior=Optional.ofNullable(imported.get(ref));if(prior.isPresent()){if(!prior.get().getFingerprint().equals(fingerprint(module,row)))throw new IllegalArgumentException("Reference already imported with different values; edit the original record instead");result.add(new RowResult(i,ref,"SKIP","Already imported",prior.get().getTargetId()));}else result.add(new RowResult(i,ref,"READY","Fields and references valid; balances checked on import",null));}catch(Exception e){result.add(new RowResult(i,ref,"ERROR",e.getMessage(),null));}i++;}return result;}
 private void validateRow(String module,Map<String,String> row,Map<String,List<Map<String,Object>>> refs){var columns=fields(module);for(var key:row.keySet())if(!columns.containsKey(key))throw new IllegalArgumentException("Unknown column: "+key);var ref=row.getOrDefault("externalReference","").trim();if(ref.isEmpty()||ref.length()>100)throw new IllegalArgumentException("externalReference is required (up to 100 characters)");for(String key:spec(module).required().split(","))if(row.getOrDefault(key,"").isBlank())throw new IllegalArgumentException(key+" is required");for(var field:columns.entrySet()){String value=row.getOrDefault(field.getKey(),"").trim();if(value.isEmpty())continue;convert(value,field.getValue());if(field.getValue()==LocalDate.class&&!field.getKey().equals("expectedDeliveryDate")&&((LocalDate)convert(value,LocalDate.class)).isAfter(BusinessTime.today()))throw new IllegalArgumentException(field.getKey()+" cannot be in the future");var allowed=choices(module,field.getKey(),field.getValue());if(!allowed.isEmpty()&&!allowed.contains(value))throw new IllegalArgumentException(field.getKey()+" must be one of "+allowed);if(refs.containsKey(field.getKey())&&refs.get(field.getKey()).stream().noneMatch(r->String.valueOf(r.get("value")).equals(value)))throw new IllegalArgumentException("Unknown "+field.getKey()+": "+value);}
  var dto=request(module,row);var violations=validator.validate(dto);if(!violations.isEmpty())throw new IllegalArgumentException(violations.stream().map(v->v.getPropertyPath()+" "+v.getMessage()).sorted().findFirst().orElse("Invalid values"));
 }
 static Object convert(String value,Class<?> type){if(value==null||value.isBlank()){if(type==boolean.class)return true;return null;}try{if(type==String.class)return value.trim();if(type==Long.class)return new BigDecimal(value).longValueExact();if(type==Integer.class)return new BigDecimal(value).intValueExact();if(type==Double.class){double n=Double.parseDouble(value);if(!Double.isFinite(n))throw new Exception();return n;}if(type==BigDecimal.class)return new BigDecimal(value);if(type==boolean.class||type==Boolean.class){if(!Set.of("true","false","1","0").contains(value.toLowerCase()))throw new Exception();return value.equalsIgnoreCase("true")||value.equals("1");}if(type==LocalDate.class){if(value.matches("[0-9]+(\\.[0-9]+)?")){var serial=new BigDecimal(value);return LocalDate.of(1899,12,30).plusDays(serial.longValueExact());}return LocalDate.parse(value);}if(type.isEnum())return Enum.valueOf((Class)type,value);throw new Exception();}catch(Exception e){throw new IllegalArgumentException("Invalid "+type.getSimpleName()+" value: "+value+". Dates must be YYYY-MM-DD.");}}
 private String value(Map<String,String> row,String name,Class<?> type){var value=row.get(name);return value==null||value.isBlank()?defaultValue(name,type):value.trim();}
 private Object request(String module,Map<String,String> row){try{var type=spec(module).type();String key="retro:"+MoneyService.fingerprint(module+":"+row.get("externalReference"));if(type.isRecord()){var parts=type.getRecordComponents();Object[] args=new Object[parts.length];Class<?>[] types=new Class<?>[parts.length];for(int i=0;i<parts.length;i++){var part=parts[i];types[i]=part.getType();args[i]=part.getName().equals("requestKey")?key:convert(value(row,part.getName(),part.getType()),part.getType());}return type.getDeclaredConstructor(types).newInstance(args);}var value=type.getDeclaredConstructor().newInstance();for(var f:type.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())){f.setAccessible(true);f.set(value,convert(value(row,f.getName(),f.getType()),f.getType()));}return value;}catch(InvocationTargetException e){throw new IllegalArgumentException(e.getCause().getMessage());}catch(ReflectiveOperationException e){throw new IllegalArgumentException("Cannot read this row: "+e.getMessage());}}
 private String fingerprint(String module,Map<String,String> row){var normalized=new TreeMap<String,String>();fields(module).forEach((key,type)->normalized.put(key,row.getOrDefault(key,defaultValue(key,type)).trim()));return MoneyService.fingerprint(normalized);}
 public List<RowResult> commit(String module,List<Map<String,String>> rows,Authentication auth){em.createNativeQuery("SELECT pg_advisory_xact_lock(734215910)").getSingleResult();var preview=preview(module,rows);var errors=preview.stream().filter(r->r.status().equals("ERROR")).toList();if(!errors.isEmpty())throw new IllegalArgumentException("Row "+errors.getFirst().row()+": "+errors.getFirst().message());var result=new ArrayList<RowResult>();for(int i=0;i<rows.size();i++){var p=preview.get(i);if(p.status().equals("SKIP")){result.add(p);continue;}try{var row=rows.get(i);var dto=request(module,row);Object target=switch(module){case "MONEY"->money.record((MoneyRequest)dto,auth);case "MONEY_ACCOUNT"->{var r=(AccountRequest)dto;yield money.createAccount(r.name(),r.kind());}case "INVENTORY_TYPE"->catalog.save(null,(InventoryCatalogService.OptionRequest)dto,auth);case "INVENTORY"->inventory.createTransaction((InventoryTransactionRequest)dto,auth);case "PRODUCTION"->production.createBatch((ProductionBatchRequest)dto,auth);case "ROOM_TRANSFER"->production.transferToLightRoom((Long)convert(row.get("batchId"),Long.class),(RoomTransferRequest)dto,auth);case "HARVEST"->harvest.createHarvest((HarvestRequest)dto,auth);case "ORDER"->orders.createOrder((CustomerOrderRequest)dto,auth);case "VENDOR"->sourcing.vendor(null,(SourcingRequests.Vendor)dto,auth);case "RECEIPT"->sourcing.receipt(null,(SourcingRequests.Receipt)dto,auth).receipt();case "PAYMENT"->sourcing.payment(null,(SourcingRequests.Payment)dto,auth);case "RECOVERY"->sourcing.recovery(null,(SourcingRequests.Recovery)dto,auth);default->throw new IllegalArgumentException("Unknown module");};String id=String.valueOf(target.getClass().getMethod("getId").invoke(target));var log=new RetroImportLog();log.setModule(module);log.setExternalReference(p.reference());log.setFingerprint(fingerprint(module,row));log.setTargetId(id);log.setImportedAt(BusinessTime.now());log.setImportedBy(auth.getName());log.setDetails(new TreeMap<>(row).toString());logs.save(log);audit.createAudit(auth,"RETRO_IMPORT","IMPORT_"+module,id,"Reference "+p.reference()+"; "+log.getDetails());result.add(new RowResult(i+1,p.reference(),"IMPORTED","Saved and audited",id));}catch(Exception e){throw new IllegalArgumentException("Row "+(i+1)+": "+e.getMessage()+". Nothing from this import was saved.",e);}}return result;}
 public List<RetroImportLog> history(){return logs.findTop100ByOrderByIdDesc();}
}
