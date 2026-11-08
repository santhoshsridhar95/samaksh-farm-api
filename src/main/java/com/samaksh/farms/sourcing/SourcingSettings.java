package com.samaksh.farms.sourcing;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="sourcing_settings") @Getter @Setter @NoArgsConstructor
public class SourcingSettings {
    @Id private Long id;
    @Column(precision=15,scale=4) private BigDecimal sellingPricePerKg;
}
