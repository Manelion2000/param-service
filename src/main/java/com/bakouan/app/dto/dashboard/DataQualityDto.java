package com.bakouan.app.dto.dashboard;

import java.math.BigDecimal;

public record DataQualityDto(
        long totalImports,
        long invalidRows,
        long validRows,
        BigDecimal parsingSuccessRate,
        long duplicateCount,
        BigDecimal duplicateRate,
        BigDecimal invalidRowRate,
        long operatorOutOfScopeCount,
        BigDecimal operatorOutOfScopeRate
) {
}
