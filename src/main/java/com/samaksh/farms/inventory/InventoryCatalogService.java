package com.samaksh.farms.inventory;
import com.samaksh.farms.audit.service.AuditService;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class InventoryCatalogService {
    private final InventoryOptionRepository options;
    private final EntityManager em;
    private final AuditService audit;
    public void lock() {
        em.createNativeQuery("SELECT pg_advisory_xact_lock(734215909)").getSingleResult();
        if (options.existsById("ITEM:SPAWN")) return;
        // The old enum check constraints cannot represent user-managed type codes.
        var constraints = em.createNativeQuery("SELECT conname FROM pg_constraint WHERE conrelid = 'inventory_transaction'::regclass AND contype = 'c' AND (pg_get_constraintdef(oid) LIKE '%inventory_type%' OR pg_get_constraintdef(oid) LIKE '%transaction_type%')").getResultList();
        for (Object name : constraints) em.createNativeQuery("ALTER TABLE inventory_transaction DROP CONSTRAINT \"" + name.toString().replace("\"", "\"\"") + "\"").executeUpdate();
        for (var item : com.samaksh.farms.enums.InventoryType.values()) seed("ITEM", item.name(), item.name(), item.name().equals("BAG") ? "pieces" : "kg", null);
        for (var movement : com.samaksh.farms.enums.TransactionType.values()) seed("MOVEMENT", movement.name(), movement.name(), null,
                Set.of("PURCHASE", "ADJUSTMENT").contains(movement.name()) ? 1 : -1);
    }
    private void seed(String category, String code, String label, String unit, Integer effect) {
        var o = new InventoryOption(); o.setId(category + ":" + code); o.setCategory(category); o.setCode(code); o.setLabel(label); o.setUnit(unit); o.setEffect(effect); o.setActive(true); options.save(o);
    }
    public List<InventoryOption> list() { lock(); return options.findAll(); }
    public InventoryOption find(String category, String code) { return options.findById(category + ":" + code).orElseThrow(() -> new IllegalArgumentException("Unknown " + category.toLowerCase() + " type")); }
    public InventoryOption save(String id, OptionRequest r, Authentication auth) {
        lock(); String code = r.code().trim().toUpperCase(Locale.ROOT); String key = r.category() + ":" + code;
        if (id == null && options.existsById(key)) throw new IllegalArgumentException("This type code already exists");
        var o = id == null ? new InventoryOption() : options.findById(id).orElseThrow(() -> new IllegalArgumentException("Type not found"));
        if (id != null && !id.equals(key)) throw new IllegalArgumentException("Type codes cannot change; edit the display name instead");
        if (r.category().equals("MOVEMENT") && !Objects.equals(r.effect(), 1) && !Objects.equals(r.effect(), -1)) throw new IllegalArgumentException("Choose stock increase or decrease");
        if (r.category().equals("ITEM") && (r.unit() == null || r.unit().isBlank())) throw new IllegalArgumentException("Unit is required");
        if (id != null && r.category().equals("MOVEMENT") && Arrays.stream(com.samaksh.farms.enums.TransactionType.values()).anyMatch(t -> t.name().equals(code)) && !Objects.equals(o.getEffect(), r.effect())) throw new IllegalArgumentException("Built-in stock directions cannot change. Add a new transaction type instead.");
        String before = o.getLabel() + ", unit " + o.getUnit() + ", effect " + o.getEffect() + ", active " + o.isActive();
        o.setId(key); o.setCategory(r.category()); o.setCode(code); o.setLabel(r.label().trim()); o.setUnit(r.unit()); o.setEffect(r.effect()); o.setActive(r.active()); options.save(o);
        audit.createAudit(auth,"INVENTORY",id == null ? "CREATE_TYPE" : "EDIT_TYPE",key,before + " -> " + r);
        return o;
    }
    public record OptionRequest(@NotNull @Pattern(regexp="ITEM|MOVEMENT") String category,
        @NotBlank @Pattern(regexp="[A-Za-z][A-Za-z0-9_]{0,39}") String code,
        @NotBlank @Size(max=100) String label, @Size(max=30) String unit, Integer effect, boolean active) {}
}
