package com.bakouan.app.dto;

import com.bakouan.app.enums.OperatorType;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ReconciliationRunRequest(
        LocalDate businessDate,
        LocalDate dateFrom,
        LocalDate dateTo,
        OperatorType operator,
        String bankImportIds,
        String moovImportIds,
        String orangeImportIds,
        @NotBlank String label
) {
}

