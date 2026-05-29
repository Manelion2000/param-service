package com.bakouan.app.dto;

import com.bakouan.app.enums.OperatorType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompensationDailyDto(
        LocalDate businessDate,
        OperatorType operator,
        long operatorSuccessCount,
        long bankSuccessCount,
        BigDecimal operatorSuccessAmount,
        BigDecimal bankSuccessAmount,
        BigDecimal difference,
        String decision
) {
}
