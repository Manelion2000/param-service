package com.bakouan.app.dto;

import com.bakouan.app.enums.ReconciliationResultType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompensationDiscrepancyDto(
        LocalDate businessDate,
        String transactionKey,
        String operationReference,
        ReconciliationResultType resultType,
        String operatorPhoneNumber,
        String bankStatusRaw,
        String operatorStatusRaw,
        BigDecimal bankAmount,
        BigDecimal operatorAmount,
        BigDecimal amountDifference,
        String reason
) {
}
