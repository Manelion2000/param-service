package com.bakouan.app.dto;

import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.SourceType;

public record ImportFullDeletionResult(
        SourceType sourceType,
        OperatorType operatorScope,
        int deletedImports,
        int deletedTransactions,
        int deletedResults,
        int deletedRuns
) {
}
