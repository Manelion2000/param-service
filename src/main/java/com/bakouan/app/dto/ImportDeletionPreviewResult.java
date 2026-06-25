package com.bakouan.app.dto;

import com.bakouan.app.enums.SourceType;

import java.time.LocalDate;
import java.util.Set;

public record ImportDeletionPreviewResult(
        SourceType sourceType,
        LocalDate businessDate,
        int candidateImports,
        int candidateTransactions,
        int impactedResults,
        int impactedRuns,
        Set<Long> candidateImportIds,
        Set<Long> impactedRunIds,
        boolean cascadeConfirmationRequired
) {
}
