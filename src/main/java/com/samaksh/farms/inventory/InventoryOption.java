package com.samaksh.farms.inventory;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="inventory_options") @Getter @Setter @NoArgsConstructor
public class InventoryOption {
    @Id private String id;
    private String category;
    private String code;
    private String label;
    private String unit;
    private Integer effect;
    private boolean active;
}
