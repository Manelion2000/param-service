package com.bakouan.app.dto;

import com.bakouan.app.enums.DataRetentionMode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RetentionExecutionDto(
        OffsetDateTime executedAt,
        LocalDate cutoffDateExclusive,
        DataRetentionMode mode,
        long archivedResults,
        long archivedRuns,
        long purgedResults,
        long purgedRuns
) {
}
