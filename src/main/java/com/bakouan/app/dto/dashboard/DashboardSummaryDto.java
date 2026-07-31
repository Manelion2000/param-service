package com.bakouan.app.dto.dashboard;

import com.bakouan.app.enums.OperatorType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSummaryDto(
        DashboardPeriodType periodType,
        LocalDate businessDate,
        LocalDate dateFrom,
        LocalDate dateTo,
        OperatorType channel,
        long totalBank,
        long totalOperator,
        long totalResults,
        long matchOk,
        long debitATort,
        long creditSansDebit,
        long echecDesDeuxCotes,
        long absentCoteOperateur,
        long absentCoteBanque,
        long montantDifferent,
        long statutInconnu,
        long doublons,
        long invalidRows,
        BigDecimal matchingRate,
        BigDecimal successRate,
        BigDecimal anomalyRate,
        BigDecimal montantGlobalBanque,
        BigDecimal montantGlobalOperateur,
        BigDecimal montantAnomalies,
        BigDecimal ecartGlobal,
        BigDecimal moovClosingBalance
) {
}
