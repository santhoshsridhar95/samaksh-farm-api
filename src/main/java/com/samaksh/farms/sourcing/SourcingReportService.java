package com.samaksh.farms.sourcing;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.finance.*;
import com.samaksh.farms.enums.ProductUnitType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class SourcingReportService {
 private final SourcingReceiptRepository receipts;
 private final SourcingVendorRepository vendors;
 private final SaleRepository sales;
 private final MoneyEntryRepository entries;
 private final SourcingPaymentRepository payments;
 private final SourcingRecoveryRepository recoveries;
 public record SourceLine(Long receiptId,LocalDate date,Long vendorId,String source,String kind,String product,BigDecimal boxes,BigDecimal kg,BigDecimal costPerBox,BigDecimal goodsCost,BigDecimal farmCosts,BigDecimal damagedBoxes){}
 public record SaleLine(Long saleId,LocalDate date,String customer,String product,String unit,BigDecimal quantity,BigDecimal revenue){}
 public record ExpenseLine(Long entryId,LocalDate date,String description,BigDecimal amount){}
 public record Day(LocalDate date,BigDecimal vendorBoxes,BigDecimal farmBoxes,BigDecimal spoiledBoxes,BigDecimal soldBoxes,BigDecimal soldKg,BigDecimal otherSoldUnits,BigDecimal revenue,BigDecimal goodsCost,BigDecimal farmCosts,BigDecimal overhead,BigDecimal grossProfit,BigDecimal netProfit){}
 private static BigDecimal n(Number n){return n==null?BigDecimal.ZERO:new BigDecimal(n.toString());}
 private static BigDecimal cash(BigDecimal n){return n.setScale(2,RoundingMode.HALF_UP);}
 private boolean within(LocalDate d,LocalDate from,LocalDate to){return d!=null&&!d.isBefore(from)&&!d.isAfter(to);}
 public Map<String,Object> report(LocalDate from,LocalDate to){
  if(from==null||to==null||to.isBefore(from))throw new IllegalArgumentException("Choose a valid date range");
  var previousFrom=from.minusMonths(1);var previousTo=to.equals(to.withDayOfMonth(to.lengthOfMonth()))?to.minusMonths(1).withDayOfMonth(to.minusMonths(1).lengthOfMonth()):to.minusMonths(1);
  var vendorMap=new HashMap<Long,SourcingVendor>();for(var v:vendors.findAll())vendorMap.put(v.getId(),v);
  var sourceLines=new ArrayList<SourceLine>();
  for(var r:receipts.findAllByOrderByDateDescIdDesc())if(within(r.getDate(),previousFrom,to)){
   var v=vendorMap.get(r.getVendorId());var goods=cash(r.getQuantityKg().multiply(r.getCostPerKg()));var farm=r.getExpensesBorneBy().equals("FARM")?r.getTransportCost().add(r.getOtherCost()):BigDecimal.ZERO;
   sourceLines.add(new SourceLine(r.getId(),r.getDate(),r.getVendorId(),v==null?"Unknown source":v.getName(),v==null?"VENDOR":v.getKind(),r.getProduct(),r.getBoxes(),r.getQuantityKg(),r.getCostPerKg().divide(r.getBoxesPerKg(),4,RoundingMode.HALF_UP),goods,farm,n(r.getDamagedBoxes())));
  }
  var saleLines=new ArrayList<SaleLine>();
  for(var s:sales.findAll())if(s.getSaleDate()!=null&&within(s.getSaleDate().toLocalDate(),previousFrom,to)&&!"COLLECTION".equals(s.getSalesChannel())){
   var unit=s.getProduct()==null||s.getProduct().getUnitType()==null?"BOX":s.getProduct().getUnitType().name();
   saleLines.add(new SaleLine(s.getId(),s.getSaleDate().toLocalDate(),s.getCustomer()==null?"Unknown customer":s.getCustomer().getCustomerName(),s.getProduct()==null?"Mushrooms":s.getProduct().getProductName(),unit,n(s.getQuantity()),cash(n(s.getTotalAmount()))));
  }
  var allEntries=entries.findAllByOrderByDateDescIdDesc();var entryMap=new HashMap<Long,MoneyEntry>();allEntries.forEach(e->entryMap.put(e.getId(),e));
  var expenseLines=new ArrayList<ExpenseLine>();
  for(var e:allEntries)if(within(e.getDate(),previousFrom,to)){
   var original="REVERSAL".equals(e.getType())?entryMap.get(e.getReversalOf()):e;
   if(original==null||!Set.of("EXPENSE","SALARY").contains(original.getType())||(original.getRequestKey()!=null&&original.getRequestKey().startsWith("source:")))continue;
   expenseLines.add(new ExpenseLine(e.getId(),e.getDate(),e.getDescription(),"REVERSAL".equals(e.getType())?e.getAmount().negate():e.getAmount()));
  }
  var dates=new TreeSet<LocalDate>();sourceLines.forEach(x->dates.add(x.date()));saleLines.forEach(x->dates.add(x.date()));expenseLines.forEach(x->dates.add(x.date()));
  var days=new ArrayList<Day>();for(var date:dates)days.add(day(date,sourceLines,saleLines,expenseLines));
  var current=days.stream().filter(d->within(d.date(),from,to)).toList();var previous=days.stream().filter(d->within(d.date(),previousFrom,previousTo)).toList();
  var allPayments=payments.findAll();var allRecoveries=recoveries.findAll();
  var advances=allPayments.stream().filter(p->!p.getDate().isAfter(to)).map(p->n(p.getAdditionalAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
  var recovered=allRecoveries.stream().filter(r->!r.getDate().isAfter(to)).map(SourcingRecovery::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
  return Map.of("days",current,"totals",total(current),"previousTotals",total(previous),"previousFrom",previousFrom,"previousTo",previousTo,
   "receipts",sourceLines.stream().filter(r->within(r.date(),from,to)).toList(),"sales",saleLines.stream().filter(r->within(r.date(),from,to)).toList(),"expenses",expenseLines.stream().filter(r->within(r.date(),from,to)).toList(),"recoverable",advances.subtract(recovered));
 }
 static Day day(LocalDate date,List<SourceLine> sources,List<SaleLine> sales,List<ExpenseLine> expenses){
  BigDecimal vendor=BigDecimal.ZERO,farm=BigDecimal.ZERO,spoiled=BigDecimal.ZERO,goods=BigDecimal.ZERO,costs=BigDecimal.ZERO,boxes=BigDecimal.ZERO,kg=BigDecimal.ZERO,other=BigDecimal.ZERO,revenue=BigDecimal.ZERO,overhead=BigDecimal.ZERO;
  for(var r:sources)if(r.date().equals(date)){if(r.kind().equals("OWN_FARM"))farm=farm.add(r.boxes());else vendor=vendor.add(r.boxes());spoiled=spoiled.add(r.damagedBoxes());goods=goods.add(r.goodsCost());costs=costs.add(r.farmCosts());}
  for(var s:sales)if(s.date().equals(date)){revenue=revenue.add(s.revenue());switch(s.unit()){case "BOX"->boxes=boxes.add(s.quantity());case "KG"->kg=kg.add(s.quantity());default->other=other.add(s.quantity());}}
  for(var e:expenses)if(e.date().equals(date))overhead=overhead.add(e.amount());
  return new Day(date,vendor,farm,spoiled,boxes,kg,other,cash(revenue),cash(goods),cash(costs),cash(overhead),cash(revenue.subtract(goods)),cash(revenue.subtract(goods).subtract(costs).subtract(overhead)));
 }
 static Map<String,BigDecimal> total(List<Day> days){var result=new LinkedHashMap<String,BigDecimal>();for(String key:List.of("vendorBoxes","farmBoxes","spoiledBoxes","soldBoxes","soldKg","otherSoldUnits","revenue","goodsCost","farmCosts","overhead","grossProfit","netProfit"))result.put(key,BigDecimal.ZERO);for(var d:days){BigDecimal[] values={d.vendorBoxes(),d.farmBoxes(),d.spoiledBoxes(),d.soldBoxes(),d.soldKg(),d.otherSoldUnits(),d.revenue(),d.goodsCost(),d.farmCosts(),d.overhead(),d.grossProfit(),d.netProfit()};int i=0;for(String key:result.keySet())result.put(key,result.get(key).add(values[i++]));}return result;}
}
