package com.bakouan.app.model;

import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationRunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reconciliation_run")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private OperatorType operator;

    private LocalDate businessDateFrom;
    private LocalDate businessDateTo;

    @Column(columnDefinition = "TEXT")
    private String bankImportIds;

    @Column(columnDefinition = "TEXT")
    private String moovImportIds;

    @Column(columnDefinition = "TEXT")
    private String orangeImportIds;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    private OffsetDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReconciliationRunStatus status;

    @Column(columnDefinition = "TEXT")
    private String summaryJson;
}

