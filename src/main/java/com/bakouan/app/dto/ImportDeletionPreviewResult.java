package com.bakouan.app.dto;

import com.bakouan.app.enums.SourceType;

import java.time.LocalDate;

public record ImportDeletionPreviewResult(
        SourceType sourceType,
        LocalDate businessDate,
        int candidateImports,
        int candidateTransactions,
        int impactedResults,
        int impactedRuns
) {
}
