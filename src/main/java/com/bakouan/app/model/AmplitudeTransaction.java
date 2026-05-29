package com.bakouan.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "amplitude_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmplitudeTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id")
    @JsonIgnore
    private FileImport fileImport;

    private String operationReference;
    private String accountingDateRaw;
    private String valueDateRaw;
    private String pieceNumber;
    private String eventNumber;
    private String phoneNumber;
    private String libelle;
    private String accountNumber;
    private String direction;
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;
    private LocalDateTime operationDate;

    @Column(columnDefinition = "TEXT")
    private String rawPayloadJson;
    private Integer lineNumber;
}
