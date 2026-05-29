package com.bakouan.app.dto;

import java.math.BigDecimal;
import java.util.List;

public record CompensationPeriodResponseDto(
        String periodType,
        String label,
        List<CompensationDailyDto> rows,
        long totalOperatorSuccessCount,
        long totalBankSuccessCount,
        BigDecimal totalOperatorSuccessAmount,
        BigDecimal totalBankSuccessAmount,
        BigDecimal totalDifference,
        String decision
) {
}
