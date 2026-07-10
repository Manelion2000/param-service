package com.bakouan.app.service;

import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.OrangeTransaction;
import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.NormalizedMoovStatus;
import com.bakouan.app.enums.NormalizedOrangeStatus;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.repositories.AmplitudeTransactionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconciliationClassificationServiceTest {

    private final AmplitudeTransactionRepository amplitudeTransactionRepository =
            Mockito.mock(AmplitudeTransactionRepository.class);
    private final ReconciliationClassificationService service =
            new ReconciliationClassificationService(BigDecimal.ZERO, amplitudeTransactionRepository);

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

    @Test
    void shouldClassifySuccessfulOperatorWithoutBankAsAbsentBank() {
        MoovTransaction moov = MoovTransaction.builder()
                .transactionStatusNormalized(NormalizedMoovStatus.SUCCESS_MOOV)
                .build();
        OrangeTransaction orange = OrangeTransaction.builder()
                .transactionStatusNormalized(NormalizedOrangeStatus.SUCCESS_ORANGE)
                .build();

        assertEquals(ReconciliationResultType.ABSENT_COTE_BANQUE, service.classify(null, moov));
        assertEquals(ReconciliationResultType.ABSENT_COTE_BANQUE, service.classify(null, orange));
    }

    @Test
    void shouldClassifyFailedOperatorWithoutBankAsOperatorNotCompletedWithoutBank() {
        MoovTransaction moov = MoovTransaction.builder()
                .transactionStatusNormalized(NormalizedMoovStatus.FAILED_MOOV)
                .build();
        OrangeTransaction orange = OrangeTransaction.builder()
                .transactionStatusNormalized(NormalizedOrangeStatus.FAILED_ORANGE)
                .build();

        assertEquals(ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE, service.classify(null, moov));
        assertEquals(ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE, service.classify(null, orange));
    }

    @Test
    void shouldClassifyCompletedMoovWithLongPhoneAsApprovisionnementRegardlessOfType() {
        MoovTransaction moov = MoovTransaction.builder()
                .transactionStatusNormalized(NormalizedMoovStatus.SUCCESS_MOOV)
                .transactionType("BANK_TO_WALLET")
                .msisdn("2267000000012345")
                .build();

        assertEquals(ReconciliationResultType.APPROVISIONNEMENT, service.classify(null, moov));
    }

    @Test
    void shouldNotClassifyFailedMoovWithLongPhoneAsApprovisionnement() {
        MoovTransaction moov = MoovTransaction.builder()
                .transactionStatusNormalized(NormalizedMoovStatus.FAILED_MOOV)
                .transactionType("BANK_TO_WALLET")
                .msisdn("2267000000012345")
                .build();

        assertEquals(ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE, service.classify(null, moov));
    }

    @Test
    void shouldClassifySuccessfulOrangeWithLongPhoneAsApprovisionnement() {
        OrangeTransaction orange = OrangeTransaction.builder()
                .transactionStatusNormalized(NormalizedOrangeStatus.SUCCESS_ORANGE)
                .senderMobileNumber("2267000000012345")
                .build();

        assertEquals(ReconciliationResultType.APPROVISIONNEMENT, service.classify(null, orange));
    }

    @Test
    void shouldNotClassifyFailedOrangeWithLongPhoneAsApprovisionnement() {
        OrangeTransaction orange = OrangeTransaction.builder()
                .transactionStatusNormalized(NormalizedOrangeStatus.FAILED_ORANGE)
                .senderMobileNumber("2267000000012345")
                .build();

        assertEquals(ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE, service.classify(null, orange));
    }

}

