package com.bakouan.app.service.impl;

import com.bakouan.app.dto.ImportBulkDeletionResult;
import com.bakouan.app.dto.ImportDeletionPreviewResult;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.repositories.AmplitudeTransactionRepository;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.FileImportRepository;
import com.bakouan.app.repositories.MoovTransactionRepository;
import com.bakouan.app.repositories.OrangeTransactionRepository;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import com.bakouan.app.repositories.ReconciliationRunRepository;
import com.bakouan.app.service.FileStorageService;
import com.bakouan.app.service.StatusNormalizationService;
import com.bakouan.app.service.parser.TransactionFileParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileImportDeletionServiceTest {

    @Mock private FileStorageService fileStorageService;
    @Mock private FileImportRepository fileImportRepository;
    @Mock private BankTransactionRepository bankTransactionRepository;
    @Mock private MoovTransactionRepository moovTransactionRepository;
    @Mock private OrangeTransactionRepository orangeTransactionRepository;
    @Mock private AmplitudeTransactionRepository amplitudeTransactionRepository;
    @Mock private ReconciliationRunRepository reconciliationRunRepository;
    @Mock private ReconciliationResultRepository reconciliationResultRepository;
    @Mock private StatusNormalizationService statusNormalizationService;

    private FileImportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileImportServiceImpl(
                List.<TransactionFileParser>of(),
                fileStorageService,
                fileImportRepository,
                bankTransactionRepository,
                moovTransactionRepository,
                orangeTransactionRepository,
                amplitudeTransactionRepository,
                reconciliationRunRepository,
                reconciliationResultRepository,
                statusNormalizationService
        );
    }

    @Test
    void shouldPreviewAndDeleteUsedImportWithTheSameCascadePlan() {
        LocalDate businessDate = LocalDate.of(2026, 4, 18);
        FileImport fileImport = FileImport.builder()
                .id(5L)
                .sourceType(SourceType.MOOV)
                .businessDate(businessDate)
                .originalFilename("moov.xls")
                .filePath("datas/moov.xls")
                .build();
        ReconciliationRun impactedRun = ReconciliationRun.builder()
                .id(20L)
                .moovImportIds("5")
                .build();

        when(fileImportRepository.findBySourceTypeAndBusinessDate(SourceType.MOOV, businessDate))
                .thenReturn(List.of(fileImport));
        when(moovTransactionRepository.countByFileImportId(5L)).thenReturn(2);
        when(reconciliationRunRepository.findAll()).thenReturn(List.of(impactedRun));
        when(reconciliationResultRepository.countByRunId(20L)).thenReturn(3);

        ImportDeletionPreviewResult preview = service.previewDeletionBySourceAndBusinessDate(
                SourceType.MOOV,
                null,
                businessDate
        );

        assertThat(preview.candidateImports()).isEqualTo(1);
        assertThat(preview.candidateTransactions()).isEqualTo(2);
        assertThat(preview.impactedRuns()).isEqualTo(1);
        assertThat(preview.impactedResults()).isEqualTo(3);
        assertThat(preview.candidateImportIds()).containsExactly(5L);
        assertThat(preview.impactedRunIds()).containsExactly(20L);
        assertThat(preview.cascadeConfirmationRequired()).isTrue();

        assertThatThrownBy(() -> service.deleteImportsBySourceAndBusinessDate(
                SourceType.MOOV,
                null,
                businessDate,
                false
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("confirmCascade=true");
        verify(reconciliationResultRepository, never()).deleteByRunId(20L);
        verify(moovTransactionRepository, never()).deleteByFileImportId(5L);

        ImportBulkDeletionResult deletion = service.deleteImportsBySourceAndBusinessDate(
                SourceType.MOOV,
                null,
                businessDate,
                true
        );

        assertThat(deletion.deletedImports()).isEqualTo(preview.candidateImports());
        assertThat(deletion.deletedTransactions()).isEqualTo(preview.candidateTransactions());
        assertThat(deletion.deletedRuns()).isEqualTo(preview.impactedRuns());
        assertThat(deletion.deletedResults()).isEqualTo(preview.impactedResults());
        verify(reconciliationResultRepository).deleteByRunId(20L);
        verify(reconciliationRunRepository).deleteAll(List.of(impactedRun));
        verify(moovTransactionRepository).deleteByFileImportId(5L);
        verify(fileImportRepository).delete(fileImport);
        verify(fileStorageService).deleteIfExists("datas/moov.xls");
    }

    @Test
    void shouldRejectBankDeletionWhenOperatorIsNotProvided() {
        assertThatThrownBy(() -> service.deleteAllImportsBySource(SourceType.BANQUE, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operator est obligatoire");

        assertThatThrownBy(() -> service.deleteImportsBySourceAndBusinessDate(SourceType.BANQUE, null, LocalDate.of(2026, 4, 18), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operator est obligatoire");
    }
}
