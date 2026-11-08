package com.samaksh.farms.retro;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="retro_import_log",uniqueConstraints=@UniqueConstraint(columnNames={"module","externalReference"})) @Getter @Setter @NoArgsConstructor
public class RetroImportLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String module;
 @Column(length=100) private String externalReference;
 private String fingerprint;
 private String targetId;
 private java.time.LocalDateTime importedAt;
 private String importedBy;
 @Column(columnDefinition="text") private String details;
}
