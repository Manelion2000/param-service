package com.bakouan.app.service.impl;

import com.bakouan.app.dto.dashboard.DashboardFilterRequest;
import com.bakouan.app.dto.dashboard.DashboardPeriodType;
import com.bakouan.app.dto.dashboard.DashboardSummaryDto;
import com.bakouan.app.dto.dashboard.DataQualityDto;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.model.FileImport;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
        assertThat(summary.montantGlobalBanque()).isEqualByComparingTo("1000");
        assertThat(summary.montantGlobalOperateur()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldComputeDashboardAmountsOnlyFromCompletedBankAndOperatorRows() {
        ReconciliationRun run = ReconciliationRun.builder()
                .id(10L)
                .operator(OperatorType.MOOV)
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
                .build();
        ReconciliationResult amountDifferent = ReconciliationResult.builder()
                .run(run)
                .resultType(ReconciliationResultType.MONTANT_DIFFERENT)
                .bankTransactionId(3L)
                .moovTransactionId(4L)
                .bankAmount(new BigDecimal("700"))
                .moovAmount(new BigDecimal("650"))
                .amountDifference(new BigDecimal("50"))
                .transactionKey("K2")
                .build();
        ReconciliationResult debitATort = ReconciliationResult.builder()
                .run(run)
                .resultType(ReconciliationResultType.DEBIT_A_TORT)
                .bankTransactionId(5L)
                .bankAmount(new BigDecimal("500"))
                .transactionKey("K3")
                .build();
        ReconciliationResult operatorNonAbouti = ReconciliationResult.builder()
                .run(run)
                .resultType(ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE)
                .moovTransactionId(6L)
                .moovAmount(new BigDecimal("900"))
                .transactionKey("K4")
                .build();

        when(runRepository.findById(10L)).thenReturn(Optional.of(run));
        when(resultRepository.findByRunIdIn(List.of(10L))).thenReturn(List.of(match, amountDifferent, debitATort, operatorNonAbouti));

        DashboardSummaryDto summary = service.summary(new DashboardFilterRequest(
                null,
                null,
                null,
                OperatorType.MOOV,
                10L,
                null
        ));

        assertThat(summary.montantGlobalBanque()).isEqualByComparingTo("1700");
        assertThat(summary.montantGlobalOperateur()).isEqualByComparingTo("1650");
        assertThat(summary.ecartGlobal()).isEqualByComparingTo("50");
        assertThat(summary.montantAnomalies()).isEqualByComparingTo("550");
    }

    @Test
    void shouldUseOnlyLatestRunWhenSeveralRunsExistForSamePeriod() {
        ReconciliationRun oldRun = ReconciliationRun.builder()
                .id(10L)
                .operator(OperatorType.MOOV)
                .businessDateFrom(LocalDate.of(2026, 3, 11))
                .businessDateTo(LocalDate.of(2026, 3, 11))
                .startedAt(OffsetDateTime.parse("2026-03-11T08:00:00Z"))
                .bankImportIds("1")
                .moovImportIds("2")
                .build();
        ReconciliationRun latestRun = ReconciliationRun.builder()
                .id(11L)
                .operator(OperatorType.MOOV)
                .businessDateFrom(LocalDate.of(2026, 3, 11))
                .businessDateTo(LocalDate.of(2026, 3, 11))
                .startedAt(OffsetDateTime.parse("2026-03-11T09:00:00Z"))
                .bankImportIds("1")
                .moovImportIds("2")
                .build();
        ReconciliationResult latestOnly = ReconciliationResult.builder()
                .run(latestRun)
                .resultType(ReconciliationResultType.MATCH_OK)
                .bankTransactionId(1L)
                .moovTransactionId(2L)
                .bankAmount(new BigDecimal("1000"))
                .moovAmount(new BigDecimal("1000"))
                .transactionKey("K1")
                .businessDate(LocalDate.of(2026, 3, 11))
                .build();

        when(runRepository.findByOperatorAndBusinessDateOverlap(OperatorType.MOOV, LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 11), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(oldRun, latestRun)));
        when(resultRepository.findByRunIdIn(List.of(11L))).thenReturn(List.of(latestOnly));

        DashboardSummaryDto summary = service.summary(new DashboardFilterRequest(
                LocalDate.of(2026, 3, 11),
                null,
                null,
                OperatorType.MOOV,
                null,
                null
        ));

        assertThat(summary.totalResults()).isEqualTo(1);
        assertThat(summary.totalBank()).isEqualTo(1);
        assertThat(summary.totalOperator()).isEqualTo(1);
        assertThat(summary.montantGlobalBanque()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldExposeFailedOperatorWithoutBankInDataQualityButExcludeItFromFinancialKpis() {
        ReconciliationRun run = ReconciliationRun.builder()
                .id(10L)
                .operator(OperatorType.MOOV)
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
                .build();
        ReconciliationResult operatorOutOfScope = ReconciliationResult.builder()
                .run(run)
                .resultType(ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE)
                .moovTransactionId(3L)
                .moovAmount(new BigDecimal("500"))
                .transactionKey("K2")
                .build();
        FileImport bankImport = FileImport.builder().id(1L).validRows(1).invalidRows(0).build();
        FileImport operatorImport = FileImport.builder().id(2L).validRows(1).invalidRows(0).build();
        DashboardFilterRequest filter = new DashboardFilterRequest(
                null,
                null,
                null,
                OperatorType.MOOV,
                10L,
                null
        );

        when(runRepository.findById(10L)).thenReturn(Optional.of(run));
        when(resultRepository.findByRunIdIn(List.of(10L))).thenReturn(List.of(match, operatorOutOfScope));
        when(fileImportRepository.findAllById(java.util.Set.of(1L, 2L)))
                .thenReturn(List.of(bankImport, operatorImport));

        DataQualityDto quality = service.dataQuality(filter);
        DashboardSummaryDto summary = service.summary(filter);

        assertThat(quality.operatorOutOfScopeCount()).isEqualTo(1);
        assertThat(quality.operatorOutOfScopeRate()).isEqualByComparingTo("50.00");
        assertThat(summary.totalResults()).isEqualTo(2);
        assertThat(summary.totalBank()).isEqualTo(1);
        assertThat(summary.totalOperator()).isEqualTo(1);
        assertThat(summary.successRate()).isEqualByComparingTo("100.00");
        assertThat(summary.montantGlobalOperateur()).isEqualByComparingTo("1000");
    }
}
