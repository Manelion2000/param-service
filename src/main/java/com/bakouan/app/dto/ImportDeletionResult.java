package com.bakouan.app.dto;

import com.bakouan.app.enums.SourceType;

public record ImportDeletionResult(
        SourceType sourceType,
        Long deletedImportId,
        String deletedFilename,
        int deletedTransactions,
        int deletedResults,
        int deletedRuns
) {
}
