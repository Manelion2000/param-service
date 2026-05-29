package com.bakouan.app.service;

import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.dto.ReconciliationRunRequest;
import com.bakouan.app.dto.ReconciliationSummaryDto;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.enums.ReconciliationResultType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ReconciliationService {
    ReconciliationRun run(ReconciliationRunRequest request);
    Page<ReconciliationRun> runs(OperatorType operator, Pageable pageable);
    Page<ReconciliationRun> runsByDateRange(OperatorType operator, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);
    ReconciliationRun runById(Long runId);
    Page<ReconciliationResult> results(Long runId, Pageable pageable);
    Page<ReconciliationResult> resultsByType(Long runId, ReconciliationResultType type, Pageable pageable);
    ReconciliationSummaryDto summary(Long runId);
    Page<ReconciliationResult> globalResults(LocalDate dateFrom, LocalDate dateTo, ReconciliationResultType type, OperatorType operator, Pageable pageable);
    ReconciliationSummaryDto globalSummary(LocalDate dateFrom, LocalDate dateTo, OperatorType operator);
}

