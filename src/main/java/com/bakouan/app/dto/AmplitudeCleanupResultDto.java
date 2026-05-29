package com.bakouan.app.dto;

import java.time.LocalDate;

public record AmplitudeCleanupResultDto(
        String mode,
        LocalDate dateFrom,
        LocalDate dateTo,
        int deletedImports,
        int deletedTransactions
) {
}

