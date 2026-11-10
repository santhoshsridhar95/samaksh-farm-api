package com.samaksh.farms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.Set;

/** Align legacy varchar columns with the text mappings; retain the complete audit trail. */
@Component
@RequiredArgsConstructor
public class CorrectionTextMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    @Override public void run(ApplicationArguments args) {
        var tables=jdbc.queryForList("""
                SELECT table_name FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name IN ('audit_log', 'sales')
                  AND column_name = 'remarks'
                  AND data_type IN ('character varying', 'character')
                """,String.class);
        for(String table:tables)if(Set.of("audit_log","sales").contains(table)) {
            jdbc.execute("ALTER TABLE \""+table+"\" ALTER COLUMN \"remarks\" TYPE text");
            org.slf4j.LoggerFactory.getLogger(CorrectionTextMigration.class).info("Migrated {}.remarks to text",table);
        }
    }
}
