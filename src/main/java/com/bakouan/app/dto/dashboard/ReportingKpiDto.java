package com.bakouan.app.dto.dashboard;

import java.math.BigDecimal;

public record ReportingKpiDto(
        long totalTransactions,
        long matchingCount,
        long anomalyCount,
        BigDecimal successRate,
        BigDecimal anomalyRate,
        long debitATortCount,
        long creditSansDebitCount,
        long absentCoteBanqueCount,
        long absentCoteOperateurCount,
        long montantDifferentCount,
        long doublonsCount,
        long statutInconnuCount,
        BigDecimal montantTotalBanque,
        BigDecimal montantTotalOperateur,
        BigDecimal montantAnomalies,
        BigDecimal ecartGlobal,
        long operateurSuccessCount,
        BigDecimal operateurSuccessAmount,
        long bankSuccessCount,
        BigDecimal bankSuccessAmount,
        long operateurSuccessSansCarthagoCount,
        BigDecimal operateurSuccessSansCarthagoAmount,
        long operateurHorsPerimetreCount,
        BigDecimal operateurHorsPerimetreAmount,
        long soldeInsuffisantCount,
        BigDecimal soldeInsuffisantAmount,
        BigDecimal soldeInsuffisantRate,
        BigDecimal moyenneJournaliereTransactions,
        LocalVolumePeakDto picVolumeJournalier
) {
}
