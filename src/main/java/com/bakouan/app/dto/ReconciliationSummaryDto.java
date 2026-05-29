package com.bakouan.app.dto;

import java.math.BigDecimal;

public record ReconciliationSummaryDto(
        long totalBank,
        long totalMoov,
        long totalMatchOk,
        long totalEchecDesDeuxCotes,
        long totalDebitATort,
        long totalCreditSansDebit,
        long totalAbsentBanque,
        long totalAbsentMoov,
        long totalMontantDifferent,
        long totalDoublons,
        BigDecimal tauxSucces,
        BigDecimal tauxEchecGlobal,
        BigDecimal tauxEchecDesDeuxCotes,
        BigDecimal tauxDebitATort,
        BigDecimal tauxCreditSansDebit,
        BigDecimal tauxAbsentBanque,
        BigDecimal tauxAbsentMoov,
        BigDecimal tauxMontantDifferent,
        BigDecimal tauxDoublons,
        BigDecimal montantGlobalBanque,
        BigDecimal montantGlobalMoov,
        BigDecimal ecartGlobal
) {
}

