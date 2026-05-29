package com.bakouan.app.service.reconciliation;

import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.NormalizedMoovStatus;
import com.bakouan.app.enums.NormalizedOrangeStatus;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.OrangeTransaction;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReconciliationResultWriterTest {

    @Test
    void shouldUseBankTransactionDateAsBusinessDate() {
        ReconciliationResultRepository repository = mock(ReconciliationResultRepository.class);
        ReconciliationResultWriter writer = new ReconciliationResultWriter(repository);

        BankTransaction bank = BankTransaction.builder()
                .transactionDate(LocalDateTime.of(2026, 3, 11, 10, 30))
                .transactionId("BK-1")
                .allocationStatusNormalized(NormalizedBankStatus.SUCCESS_BANK)
                .build();
        MoovTransaction moov = MoovTransaction.builder()
                .completionTime(LocalDateTime.of(2026, 3, 12, 8, 0))
                .receiptNo("MV-1")
                .transactionStatusNormalized(NormalizedMoovStatus.SUCCESS_MOOV)
                .build();
        ReconciliationRun run = ReconciliationRun.builder()
                .businessDateFrom(LocalDate.of(2026, 3, 10))
                .build();

        writer.save(run, "K1", bank, moov, ReconciliationResultType.MATCH_OK, null);

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBusinessDate()).isEqualTo(LocalDate.of(2026, 3, 11));
    }

    @Test
    void shouldUseOrangeTransactionDateWhenBankDateMissing() {
        ReconciliationResultRepository repository = mock(ReconciliationResultRepository.class);
        ReconciliationResultWriter writer = new ReconciliationResultWriter(repository);

        FileImport bankImport = FileImport.builder()
                .businessDate(LocalDate.of(2026, 3, 1))
                .build();
        BankTransaction bank = BankTransaction.builder()
                .fileImport(bankImport)
                .transactionId("BK-2")
                .allocationStatusNormalized(NormalizedBankStatus.SUCCESS_BANK)
                .build();
        OrangeTransaction orange = OrangeTransaction.builder()
                .transactionDateTime(LocalDateTime.of(2026, 3, 14, 9, 0))
                .omTransactionId("OM-1")
                .aliasBankAccountNumber("ACC-1")
                .transactionStatusNormalized(NormalizedOrangeStatus.SUCCESS_ORANGE)
                .build();
        ReconciliationRun run = ReconciliationRun.builder()
                .businessDateFrom(LocalDate.of(2026, 3, 10))
                .build();

        writer.save(run, "K2", bank, orange, ReconciliationResultType.MATCH_OK, null);

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBusinessDate()).isEqualTo(LocalDate.of(2026, 3, 14));
    }
}
