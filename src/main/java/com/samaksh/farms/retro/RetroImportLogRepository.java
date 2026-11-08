package com.samaksh.farms.retro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RetroImportLogRepository extends JpaRepository<RetroImportLog,Long> {
 Optional<RetroImportLog> findByModuleAndExternalReference(String module,String externalReference);
 List<RetroImportLog> findByModuleAndExternalReferenceIn(String module,java.util.Collection<String> references);
 List<RetroImportLog> findTop100ByOrderByIdDesc();
}
