package com.bakouan.app.model;

import com.bakouan.app.enums.ReconciliationResultType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reconciliation_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    @JsonIgnore
    private ReconciliationRun run;

    private LocalDate businessDate;

    @Column(nullable = false)
    private String transactionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReconciliationResultType resultType;

    private Long bankTransactionId;
    private Long moovTransactionId;

    private String bankStatusRaw;
    private String moovStatusRaw;

    @Column(precision = 19, scale = 2)
    private BigDecimal bankAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal moovAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal amountDifference;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}

