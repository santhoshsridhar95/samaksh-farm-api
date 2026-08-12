package com.samaksh.farms.entitlement.entity;

import com.samaksh.farms.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "entitlements",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "role",
                        "permissionKey"
                }
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String permissionKey;

    @Column(nullable = false)
    private Boolean enabled;
}
