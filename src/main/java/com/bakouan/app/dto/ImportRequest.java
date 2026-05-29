package com.bakouan.app.dto;

import com.bakouan.app.enums.SourceType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ImportRequest(
        @NotNull SourceType sourceType,
        @NotNull LocalDate businessDate
) {
}

