package com.bakouan.app.model;

import com.bakouan.app.enums.NormalizedBankStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id")
    @JsonIgnore
    private FileImport fileImport;

    @Column(nullable = false)
    private String transactionId;

    private String allocationStatusRaw;
    private String rejectReasonRaw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NormalizedBankStatus allocationStatusNormalized;

    private String fullName;

    private String accountNumber;
    private String phoneNumber;
    private String operationReference;
    private String operationNature;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private LocalDateTime transactionDate;

    @Column(columnDefinition = "TEXT")
    private String rawPayloadJson;

    private Integer lineNumber;
}

