package com.bakouan.app.model;

import com.bakouan.app.enums.NormalizedOrangeStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orange_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrangeTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id")
    @JsonIgnore
    private FileImport fileImport;

    @Column(nullable = false)
    private String omTransactionId;

    @Column(nullable = false)
    private String aliasBankAccountNumber;

    private String senderMobileNumber;

    private String transactionStatusRaw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NormalizedOrangeStatus transactionStatusNormalized;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private LocalDateTime transactionDateTime;

    @Column(columnDefinition = "TEXT")
    private String rawPayloadJson;

    private Integer lineNumber;
}
