package com.bakouan.app.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TopAnomalyDto(
        String transactionKey,
        LocalDate businessDate,
        DashboardResultTypeView resultType,
        String bankStatusRaw,
        String operatorStatusRaw,
        BigDecimal bankAmount,
        BigDecimal operatorAmount,
        BigDecimal amountDifference,
        String reason
) {
}

