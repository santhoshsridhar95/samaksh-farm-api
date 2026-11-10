package com.samaksh.farms.config;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.mockito.Mockito.*;
class CorrectionTextMigrationTest {
 @Test void widensLegacyRemarksWithoutDeletingOrTruncatingRecords(){var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForList(anyString(),eq(String.class))).thenReturn(List.of("audit_log","sales"));new CorrectionTextMigration(jdbc).run(null);verify(jdbc).execute("ALTER TABLE \"audit_log\" ALTER COLUMN \"remarks\" TYPE text");verify(jdbc).execute("ALTER TABLE \"sales\" ALTER COLUMN \"remarks\" TYPE text");}
 @Test void alreadyMigratedSchemaDoesNotRunDdl(){var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForList(anyString(),eq(String.class))).thenReturn(List.of());new CorrectionTextMigration(jdbc).run(null);verify(jdbc,never()).execute(anyString());}
}
