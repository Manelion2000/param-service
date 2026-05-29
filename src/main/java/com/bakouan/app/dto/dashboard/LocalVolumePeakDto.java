package com.bakouan.app.dto.dashboard;

import java.time.LocalDate;

public record LocalVolumePeakDto(
        LocalDate businessDate,
        long totalTransactions
) {
}
