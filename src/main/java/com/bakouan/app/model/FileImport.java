package com.bakouan.app.model;

import com.bakouan.app.enums.ImportStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "file_import")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private OperatorType operatorScope;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    private String contentType;
    private String checksum;

    @Column(nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false)
    private OffsetDateTime importedAt;

    private Integer totalRows;
    private Integer validRows;
    private Integer invalidRows;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ImportStatus importStatus;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private String filePath;
}

