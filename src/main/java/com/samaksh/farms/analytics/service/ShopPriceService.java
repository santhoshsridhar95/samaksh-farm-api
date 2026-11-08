package com.samaksh.farms.analytics.service;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.enums.ExchangeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ShopPriceService {
 private final SaleRepository sales;
 public record Line(Long saleId,Long customerId,String shop,String category,String location,LocalDate date,String unit,BigDecimal quantity,BigDecimal unitPrice,BigDecimal exchangeBoxes,String exchangeType,BigDecimal exchangeCredit,BigDecimal grossValue,BigDecimal revenue,BigDecimal collected,BigDecimal pending){}
 public List<Line> report(LocalDate from,LocalDate to){
  if(from!=null&&to!=null&&to.isBefore(from))throw new IllegalArgumentException("End date must follow start date");
  var records=from!=null&&to!=null?sales.findBySaleDateGreaterThanEqualAndSaleDateLessThan(from.atStartOfDay(),to.plusDays(1).atStartOfDay()):sales.findAll();
  return records.stream().filter(s->!"COLLECTION".equals(s.getSalesChannel())&&s.getQuantity()!=null&&s.getQuantity()>0)
   .filter(s->from==null||(s.getSaleDate()!=null&&!s.getSaleDate().toLocalDate().isBefore(from)))
   .filter(s->to==null||(s.getSaleDate()!=null&&!s.getSaleDate().toLocalDate().isAfter(to)))
   .map(ShopPriceService::line).toList();
 }
 static BigDecimal n(Double value){return value==null?BigDecimal.ZERO:BigDecimal.valueOf(value);}
 static BigDecimal cash(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP);}
 static Line line(Sale s){
  var quantity=n(s.getQuantity());var price=n(s.getUnitPrice());var gross=cash(quantity.multiply(price));
  var exchange=n(s.getExchangeBoxes());var type=s.getExchangeType()==null?ExchangeType.NONE:s.getExchangeType();
  var factor=type==ExchangeType.ONE_ON_ONE?BigDecimal.ONE:type==ExchangeType.TWO_ON_ONE?new BigDecimal("0.5"):BigDecimal.ZERO;
  var credit=cash(exchange.multiply(price).multiply(factor)).min(gross).max(BigDecimal.ZERO);
  var customer=s.getCustomer();
  return new Line(s.getId(),customer==null?null:customer.getId(),customer==null?"Unknown shop":customer.getCustomerName(),customer==null?null:String.valueOf(customer.getShopCategory()),customer==null?null:customer.getLocation(),s.getSaleDate()==null?null:s.getSaleDate().toLocalDate(),s.getProduct()==null||s.getProduct().getUnitType()==null?"BOX":s.getProduct().getUnitType().name(),quantity,price,exchange,type.name(),credit,gross,cash(s.getTotalAmount()==null?gross.subtract(credit):n(s.getTotalAmount())),cash(n(s.getAmountCollected())),cash(n(s.getPendingAmount())));
 }
}
