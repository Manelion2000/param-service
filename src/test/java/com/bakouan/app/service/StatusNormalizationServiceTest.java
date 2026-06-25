package com.bakouan.app.service;

import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.NormalizedMoovStatus;
import com.bakouan.app.enums.NormalizedOrangeStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusNormalizationServiceTest {

    private final StatusNormalizationService service = new StatusNormalizationService();

    @Test
    void shouldNormalizeBankStatus() {
        assertEquals(NormalizedBankStatus.SUCCESS_BANK, service.normalizeBankStatus("Alloue"));
        assertEquals(NormalizedBankStatus.SUCCESS_BANK, service.normalizeBankStatus("Allocated"));
        assertEquals(NormalizedBankStatus.SUCCESS_BANK, service.normalizeBankStatus("Deallocated"));
        assertEquals(NormalizedBankStatus.SUCCESS_BANK, service.normalizeBankStatus("PaymentIssued"));
        assertEquals(NormalizedBankStatus.FAILED_BANK, service.normalizeBankStatus("Non Alloue"));
        assertEquals(NormalizedBankStatus.FAILED_BANK, service.normalizeBankStatus("PaymentRejected"));
        assertEquals(NormalizedBankStatus.FAILED_BANK, service.normalizeBankStatus("AllocationFailed"));
    }

    @Test
    void shouldNormalizeMoovStatus() {
        assertEquals(NormalizedMoovStatus.SUCCESS_MOOV, service.normalizeMoovStatus("Completed"));
        assertEquals(NormalizedMoovStatus.FAILED_MOOV, service.normalizeMoovStatus("Cancelled"));
    }

    @Test
    void shouldNormalizeOrangeStatus() {
        assertEquals(NormalizedOrangeStatus.SUCCESS_ORANGE, service.normalizeOrangeStatus("TS"));
        assertEquals(NormalizedOrangeStatus.FAILED_ORANGE, service.normalizeOrangeStatus("TF"));
    }
}


