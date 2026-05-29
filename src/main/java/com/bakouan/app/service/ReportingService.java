package com.bakouan.app.service;

import com.bakouan.app.dto.dashboard.ReportingPeriodType;
import com.bakouan.app.dto.dashboard.ReportingSummaryDto;
import com.bakouan.app.enums.OperatorType;

import java.time.LocalDate;

public interface ReportingService {
    ReportingSummaryDto buildSummary(ReportingPeriodType periodType, LocalDate referenceDate, OperatorType channel);

    byte[] exportExcel(ReportingPeriodType periodType, LocalDate referenceDate, OperatorType channel);

    byte[] exportPdf(ReportingPeriodType periodType, LocalDate referenceDate, OperatorType channel);
}
