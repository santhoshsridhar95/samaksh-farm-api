package com.samaksh.farms.sourcing;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="sourcing_vendors") @Getter @Setter @NoArgsConstructor
public class SourcingVendor {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(unique=true, nullable=false) private String requestKey;
    private String requestFingerprint;
    private String name;
    private String kind;
    private String phone;
    private String address;
    @Column(precision=15,scale=4) private BigDecimal boxesPerKg;
    @Column(precision=15,scale=4) private BigDecimal costPerKg;
    private String expensesBorneBy;
    private boolean active;
}
