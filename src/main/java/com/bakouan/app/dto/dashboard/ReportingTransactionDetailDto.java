package com.bakouan.app.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportingTransactionDetailDto(
        LocalDate businessDate,
        String transactionKey,
        DashboardResultTypeView resultType,
        String direction,
        String bankStatus,
        String operatorStatus,
        BigDecimal bankAmount,
        BigDecimal operatorAmount,
        BigDecimal amountDifference,
        String reason
) {
}
