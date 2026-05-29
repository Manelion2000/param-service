package com.bakouan.app.dto.dashboard;

import com.bakouan.app.enums.OperatorType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ReportingSummaryDto(
        ReportingPeriodType periodType,
        LocalDate referenceDate,
        LocalDate dateFrom,
        LocalDate dateTo,
        OperatorType channel,
        OffsetDateTime generatedAt,
        ReportingKpiDto kpis,
        List<ResultDistributionDto> distribution,
        List<ReportingDailyBreakdownDto> dailyBreakdown,
        List<ReportingTransactionDetailDto> transactionDetails
) {
}
