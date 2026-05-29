package com.bakouan.app.service;

import com.bakouan.app.dto.RetentionExecutionDto;
import com.bakouan.app.enums.DataRetentionMode;

import java.time.LocalDate;

public interface ReconciliationRetentionService {
    RetentionExecutionDto executeRetention(LocalDate cutoffDateExclusive);
    RetentionExecutionDto executeRetention(LocalDate cutoffDateExclusive, DataRetentionMode modeOverride);
    RetentionExecutionDto executeRetentionWithDefaultCutoff();
    RetentionExecutionDto executeRetentionWithDefaultCutoff(DataRetentionMode modeOverride);
}
