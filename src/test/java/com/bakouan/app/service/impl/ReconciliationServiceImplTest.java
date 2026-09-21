package com.bakouan.app.service.impl;

import com.bakouan.app.dto.ReconciliationRunRequest;
import com.bakouan.app.enums.ImportStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.enums.ReconciliationRunStatus;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.FileImportRepository;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import com.bakouan.app.repositories.ReconciliationRunRepository;
import com.bakouan.app.service.reconciliation.ReconciliationOperatorStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReconciliationServiceImplTest {

    private final FileImportRepository fileImportRepository = mock(FileImportRepository.class);
    private final BankTransactionRepository bankTransactionRepository = mock(BankTransactionRepository.class);
    private final ReconciliationRunRepository runRepository = mock(ReconciliationRunRepository.class);
    private final ReconciliationResultRepository resultRepository = mock(ReconciliationResultRepository.class);
    private final ReconciliationOperatorStrategy moovStrategy = mock(ReconciliationOperatorStrategy.class);
    private final ReconciliationServiceImpl service = new ReconciliationServiceImpl(
            fileImportRepository,
            bankTransactionRepository,
            runRepository,
            resultRepository,
            List.of(moovStrategy)
    );

    @Test
    void shouldRejectRunWithoutBankImport() {
        LocalDate businessDate = LocalDate.of(2026, 4, 24);
        when(moovStrategy.operatorSourceType()).thenReturn(SourceType.MOOV);
        when(fileImportRepository.findBySourceTypeAndOperatorScopeAndBusinessDateBetween(SourceType.BANQUE, OperatorType.MOOV, businessDate, businessDate))
                .thenReturn(List.of());
        when(moovStrategy.findImports(businessDate, businessDate)).thenReturn(List.of(importRow(20L, SourceType.MOOV, null)));

        assertThatThrownBy(() -> service.run(request(businessDate)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucun import Banque");

        verify(runRepository, never()).save(any(ReconciliationRun.class));
    }

    @Test
    void shouldRejectRunWhenSameImportsAlreadyCompleted() {
        LocalDate businessDate = LocalDate.of(2026, 4, 24);
        FileImport bankImport = importRow(10L, SourceType.BANQUE, OperatorType.MOOV);
        FileImport moovImport = importRow(20L, SourceType.MOOV, null);
        ReconciliationRun previousRun = ReconciliationRun.builder()
                .operator(OperatorType.MOOV)
                .businessDateFrom(businessDate)
                .businessDateTo(businessDate)
                .bankImportIds("10")
                .moovImportIds("20")
                .startedAt(OffsetDateTime.now())
                .status(ReconciliationRunStatus.COMPLETED)
                .build();

        when(moovStrategy.operatorSourceType()).thenReturn(SourceType.MOOV);
        when(fileImportRepository.findBySourceTypeAndOperatorScopeAndBusinessDateBetween(SourceType.BANQUE, OperatorType.MOOV, businessDate, businessDate))
                .thenReturn(List.of(bankImport));
        when(moovStrategy.findImports(businessDate, businessDate)).thenReturn(List.of(moovImport));
        when(runRepository.findByOperatorAndBusinessDateOverlap(eq(OperatorType.MOOV), eq(businessDate), eq(businessDate), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(previousRun)));

        assertThatThrownBy(() -> service.run(request(businessDate)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("deja ete rapproches");

        verify(runRepository, never()).save(any(ReconciliationRun.class));
    }

    @Test
    void shouldIgnoreInvalidImportsWhenCheckingAlreadyCompletedRuns() {
        LocalDate businessDate = LocalDate.of(2026, 4, 24);
        FileImport bankImport = importRow(10L, SourceType.BANQUE, OperatorType.MOOV);
        FileImport invalidBankImport = importRow(11L, SourceType.BANQUE, OperatorType.MOOV, 0);
        FileImport moovImport = importRow(20L, SourceType.MOOV, null);
        FileImport invalidMoovImport = importRow(21L, SourceType.MOOV, null, 0);
        ReconciliationRun previousRun = ReconciliationRun.builder()
                .operator(OperatorType.MOOV)
                .businessDateFrom(businessDate)
                .businessDateTo(businessDate)
                .bankImportIds("10")
                .moovImportIds("20")
                .startedAt(OffsetDateTime.now())
                .status(ReconciliationRunStatus.COMPLETED)
                .build();

        when(moovStrategy.operatorSourceType()).thenReturn(SourceType.MOOV);
        when(fileImportRepository.findBySourceTypeAndOperatorScopeAndBusinessDateBetween(SourceType.BANQUE, OperatorType.MOOV, businessDate, businessDate))
                .thenReturn(List.of(bankImport, invalidBankImport));
        when(moovStrategy.findImports(businessDate, businessDate)).thenReturn(List.of(moovImport, invalidMoovImport));
        when(runRepository.findByOperatorAndBusinessDateOverlap(eq(OperatorType.MOOV), eq(businessDate), eq(businessDate), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(previousRun)));

        assertThatThrownBy(() -> service.run(request(businessDate)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("deja ete rapproches");

        verify(runRepository, never()).save(any(ReconciliationRun.class));
    }

    @Test
    void shouldReturnLatestGlobalResultsPerBusinessDate() {
        LocalDate firstDate = LocalDate.of(2026, 4, 22);
        LocalDate secondDate = LocalDate.of(2026, 4, 23);
        LocalDate runDate = LocalDate.of(2026, 9, 20);
        ReconciliationRun oldFirstDateRun = run(1L, runDate, "2026-09-20T08:00:00Z");
        ReconciliationRun latestFirstDateRun = run(2L, runDate, "2026-09-20T09:00:00Z");
        ReconciliationRun secondDateRun = run(3L, runDate, "2026-09-20T07:00:00Z");
        ReconciliationResult oldResult = result(100L, oldFirstDateRun, firstDate, "OLD");
        ReconciliationResult latestResult = result(101L, latestFirstDateRun, firstDate, "LATEST");
        ReconciliationResult secondDateResult = result(102L, secondDateRun, secondDate, "SECOND");

        when(runRepository.findByOperator(OperatorType.MOOV, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(oldFirstDateRun, latestFirstDateRun, secondDateRun)));
        when(resultRepository.countByRunId(1L)).thenReturn(1);
        when(resultRepository.countByRunId(2L)).thenReturn(1);
        when(resultRepository.countByRunId(3L)).thenReturn(1);
        when(resultRepository.findByRunIdIn(org.mockito.ArgumentMatchers.argThat(ids -> ids.containsAll(List.of(1L, 2L, 3L)) && ids.size() == 3)))
                .thenReturn(List.of(oldResult, latestResult, secondDateResult));

        List<ReconciliationResult> rows = service.globalResults(null, null, null, OperatorType.MOOV, Pageable.unpaged()).getContent();

        assertThat(rows).extracting(ReconciliationResult::getTransactionKey)
                .containsExactly("SECOND", "LATEST");
    }

    @Test
    void shouldFilterLatestGlobalResultsByResultBusinessDate() {
        LocalDate includedDate = LocalDate.of(2026, 9, 12);
        LocalDate excludedDate = LocalDate.of(2026, 9, 13);
        LocalDate runDate = LocalDate.of(2026, 9, 20);
        ReconciliationRun run = run(1L, runDate, "2026-09-20T08:00:00Z");
        ReconciliationResult includedResult = result(100L, run, includedDate, "INCLUDED");
        ReconciliationResult excludedResult = result(101L, run, excludedDate, "EXCLUDED");

        when(runRepository.findByOperator(OperatorType.MOOV, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(run)));
        when(resultRepository.countByRunId(1L)).thenReturn(1);
        when(resultRepository.findByRunIdIn(org.mockito.ArgumentMatchers.argThat(ids -> ids.contains(1L) && ids.size() == 1)))
                .thenReturn(List.of(includedResult, excludedResult));

        List<ReconciliationResult> rows = service.globalResults(includedDate, includedDate, null, OperatorType.MOOV, Pageable.unpaged()).getContent();

        assertThat(rows).extracting(ReconciliationResult::getTransactionKey)
                .containsExactly("INCLUDED");
    }

    private ReconciliationRunRequest request(LocalDate businessDate) {
        return new ReconciliationRunRequest(
                businessDate,
                null,
                null,
                OperatorType.MOOV,
                null,
                null,
                null,
                "RUN-" + businessDate
        );
    }

    private FileImport importRow(Long id, SourceType sourceType, OperatorType operatorScope) {
        return importRow(id, sourceType, operatorScope, 1);
    }

    private FileImport importRow(Long id, SourceType sourceType, OperatorType operatorScope, int validRows) {
        return FileImport.builder()
                .id(id)
                .sourceType(sourceType)
                .operatorScope(operatorScope)
                .originalFilename(sourceType + ".csv")
                .storedFilename(id + ".csv")
                .businessDate(LocalDate.of(2026, 4, 24))
                .importedAt(OffsetDateTime.now())
                .validRows(validRows)
                .invalidRows(validRows == 0 ? 1 : 0)
                .totalRows(1)
                .importStatus(ImportStatus.SUCCESS)
                .filePath("datas/" + id + ".csv")
                .build();
    }

    private ReconciliationRun run(Long id, LocalDate businessDate, String startedAt) {
        return ReconciliationRun.builder()
                .id(id)
                .operator(OperatorType.MOOV)
                .businessDateFrom(businessDate)
                .businessDateTo(businessDate)
                .startedAt(OffsetDateTime.parse(startedAt))
                .status(ReconciliationRunStatus.COMPLETED)
                .build();
    }

    private ReconciliationResult result(Long id, ReconciliationRun run, LocalDate businessDate, String key) {
        return ReconciliationResult.builder()
                .id(id)
                .run(run)
                .businessDate(businessDate)
                .transactionKey(key)
                .resultType(ReconciliationResultType.MATCH_OK)
                .build();
    }
}
