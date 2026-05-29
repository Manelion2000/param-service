package com.bakouan.app.service;

import com.bakouan.app.dto.AmplitudeCleanupResultDto;

import java.time.LocalDate;

public interface AmplitudeCleanupService {
    AmplitudeCleanupResultDto cleanupAll();
    AmplitudeCleanupResultDto cleanupDaily(LocalDate businessDate);
    AmplitudeCleanupResultDto cleanupRange(LocalDate dateFrom, LocalDate dateTo);
    AmplitudeCleanupResultDto cleanupWeekly(LocalDate referenceDate);
}
