package com.bakouan.app.service.impl;

import com.bakouan.app.dto.ReconciliationRunRequest;
import com.bakouan.app.enums.ImportStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationRunStatus;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.FileImport;
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
        return FileImport.builder()
                .id(id)
                .sourceType(sourceType)
                .operatorScope(operatorScope)
                .originalFilename(sourceType + ".csv")
                .storedFilename(id + ".csv")
                .businessDate(LocalDate.of(2026, 4, 24))
                .importedAt(OffsetDateTime.now())
                .validRows(1)
                .invalidRows(0)
                .totalRows(1)
                .importStatus(ImportStatus.SUCCESS)
                .filePath("datas/" + id + ".csv")
                .build();
    }
}
