package com.samaksh.farms.sourcing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SourcingVendorRepository extends JpaRepository<SourcingVendor,Long> { Optional<SourcingVendor> findByRequestKey(String key); }
