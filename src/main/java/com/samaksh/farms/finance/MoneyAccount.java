package com.samaksh.farms.finance;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "money_accounts")
@Getter @Setter @NoArgsConstructor
public class MoneyAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String accountKey;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String kind;
    private Long userId;
}
