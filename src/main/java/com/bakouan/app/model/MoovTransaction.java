package com.bakouan.app.model;

import com.bakouan.app.enums.NormalizedMoovStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "moov_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoovTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id")
    @JsonIgnore
    private FileImport fileImport;

    @Column(nullable = false)
    private String receiptNo;

    private String transactionStatusRaw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NormalizedMoovStatus transactionStatusNormalized;

    @Column(length = 32)
    private String transactionType;

    private String msisdn;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    private LocalDateTime initiationTime;
    private LocalDateTime completionTime;

    @Column(columnDefinition = "TEXT")
    private String rawPayloadJson;

    private Integer lineNumber;
}

