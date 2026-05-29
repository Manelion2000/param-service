package com.bakouan.app.dto;

import com.bakouan.app.enums.SourceType;

import java.time.LocalDate;

public record ImportBulkDeletionResult(
        SourceType sourceType,
        LocalDate businessDate,
        int deletedImports,
        int deletedTransactions,
        int deletedResults,
        int deletedRuns
) {
}
