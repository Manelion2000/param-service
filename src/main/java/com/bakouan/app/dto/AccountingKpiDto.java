package com.bakouan.app.dto;

import java.math.BigDecimal;

public record AccountingKpiDto(
        long totalTransactions,
        BigDecimal totalAmount,
        long comptabilizedTransactions,
        BigDecimal comptabilizedAmount,
        BigDecimal comptabilizationRateCount,
        BigDecimal comptabilizationRateAmount,
        long nonComptabilizedTransactions,
        BigDecimal nonComptabilizedAmount,
        BigDecimal nonComptabilizationRateCount,
        BigDecimal amountAtRisk,
        BigDecimal netGapAmount,
        BigDecimal absoluteGapAmount,
        long transactionsWithGapCount,
        BigDecimal totalGapAmount
) {
}

