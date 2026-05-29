package com.bakouan.app.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportingDailyBreakdownDto(
        LocalDate businessDate,
        long totalTransactions,
        long matchingCount,
        long anomalyCount,
        BigDecimal successRate,
        BigDecimal montantBanque,
        BigDecimal montantOperateur,
        BigDecimal ecart
) {
}
