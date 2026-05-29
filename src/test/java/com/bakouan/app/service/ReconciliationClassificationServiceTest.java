package com.bakouan.app.service;

import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.OrangeTransaction;
import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.NormalizedMoovStatus;
import com.bakouan.app.enums.NormalizedOrangeStatus;
import com.bakouan.app.enums.ReconciliationResultType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconciliationClassificationServiceTest {

    private final ReconciliationClassificationService service = new ReconciliationClassificationService(BigDecimal.ZERO);

    @Test
    void shouldClassifyMatchOk() {
        BankTransaction bank = BankTransaction.builder()
                .allocationStatusNormalized(NormalizedBankStatus.SUCCESS_BANK)
                .amount(new BigDecimal("1000"))
                .build();
        MoovTransaction moov = MoovTransaction.builder()
                .transactionStatusNormalized(NormalizedMoovStatus.SUCCESS_MOOV)
                .amount(new BigDecimal("1000"))
                .build();

        assertEquals(ReconciliationResultType.MATCH_OK, service.classify(bank, moov));
    }

    @Test
    void shouldClassifyAmountDifference() {
        BankTransaction bank = BankTransaction.builder()
                .allocationStatusNormalized(NormalizedBankStatus.SUCCESS_BANK)
                .amount(new BigDecimal("1000"))
                .build();
        MoovTransaction moov = MoovTransaction.builder()
                .transactionStatusNormalized(NormalizedMoovStatus.SUCCESS_MOOV)
                .amount(new BigDecimal("1200"))
                .build();

        assertEquals(ReconciliationResultType.MONTANT_DIFFERENT, service.classify(bank, moov));
    }

    @Test
    void shouldClassifyOrangeMatchOk() {
        BankTransaction bank = BankTransaction.builder()
                .allocationStatusNormalized(NormalizedBankStatus.SUCCESS_BANK)
                .build();
        OrangeTransaction orange = OrangeTransaction.builder()
                .transactionStatusNormalized(NormalizedOrangeStatus.SUCCESS_ORANGE)
                .build();

        assertEquals(ReconciliationResultType.MATCH_OK, service.classify(bank, orange));
    }
}

