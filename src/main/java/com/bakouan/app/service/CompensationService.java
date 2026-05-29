package com.bakouan.app.service;

import com.bakouan.app.dto.CompensationDailyDto;
import com.bakouan.app.dto.CompensationPeriodResponseDto;
import com.bakouan.app.enums.OperatorType;

import java.time.LocalDate;
import java.util.List;

public interface CompensationService {
    List<CompensationDailyDto> daily(LocalDate dateFrom, LocalDate dateTo, OperatorType operator);
    CompensationPeriodResponseDto weekly(LocalDate dateFrom, LocalDate dateTo, OperatorType operator);
    CompensationPeriodResponseDto monthly(int year, int month, OperatorType operator);
}
