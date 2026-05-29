package com.bakouan.app.service.impl;

import com.bakouan.app.dto.dashboard.DashboardFilterRequest;
import com.bakouan.app.dto.dashboard.DashboardPeriodType;
import com.bakouan.app.dto.dashboard.DashboardSummaryDto;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private ReconciliationRunRepository runRepository;
    @Mock
    private ReconciliationResultRepository resultRepository;
    @Mock
    private FileImportRepository fileImportRepository;
    @Mock
    private BankTransactionRepository bankTransactionRepository;
    @Mock
    private MoovTransactionRepository moovTransactionRepository;
    @Mock
    private OrangeTransactionRepository orangeTransactionRepository;

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(
                runRepository,
                resultRepository,
                fileImportRepository,
                bankTransactionRepository,
                moovTransactionRepository,
                orangeTransactionRepository
        );
    }

    @Test
    void shouldBuildSummaryForMoovChannel() {
        ReconciliationRun run = ReconciliationRun.builder()
                .id(10L)
                .operator(OperatorType.MOOV)
                .businessDateFrom(LocalDate.of(2026, 3, 11))
                .bankImportIds("1")
                .moovImportIds("2")
                .build();
        ReconciliationResult match = ReconciliationResult.builder()
                .run(run)
                .resultType(ReconciliationResultType.MATCH_OK)
                .bankTransactionId(1L)
                .moovTransactionId(2L)
                .bankAmount(new BigDecimal("1000"))
                .moovAmount(new BigDecimal("1000"))
                .transactionKey("K1")
                .businessDate(LocalDate.of(2026, 3, 11))
                .build();
        ReconciliationResult debit = ReconciliationResult.builder()
                .run(run)
                .resultType(ReconciliationResultType.DEBIT_A_TORT)
                .bankTransactionId(3L)
                .bankAmount(new BigDecimal("500"))
                .transactionKey("K2")
                .businessDate(LocalDate.of(2026, 3, 11))
                .build();

        when(runRepository.findByOperatorAndBusinessDateOverlap(OperatorType.MOOV, LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 11), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(run)));
        when(resultRepository.findByRunIdIn(List.of(10L))).thenReturn(List.of(match, debit));
        when(fileImportRepository.findBySourceTypeAndOperatorScopeAndBusinessDateBetween(com.bakouan.app.enums.SourceType.BANQUE, OperatorType.MOOV, LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 11)))
                .thenReturn(List.of());
        when(fileImportRepository.findBySourceTypeAndBusinessDateBetween(com.bakouan.app.enums.SourceType.MOOV, LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 11)))
                .thenReturn(List.of());

        DashboardSummaryDto summary = service.summary(new DashboardFilterRequest(
                LocalDate.of(2026, 3, 11),
                null,
                null,
                OperatorType.MOOV,
                null,
                null
        ));

        assertThat(summary.totalBank()).isEqualTo(2);
        assertThat(summary.totalOperator()).isEqualTo(1);
        assertThat(summary.periodType()).isEqualTo(DashboardPeriodType.DAY);
        assertThat(summary.totalResults()).isEqualTo(2);
        assertThat(summary.matchOk()).isEqualTo(1);
        assertThat(summary.debitATort()).isEqualTo(1);
        assertThat(summary.montantGlobalBanque()).isEqualByComparingTo("1500");
        assertThat(summary.montantGlobalOperateur()).isEqualByComparingTo("1000");
    }
}
