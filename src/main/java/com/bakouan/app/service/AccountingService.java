package com.bakouan.app.service;

import com.bakouan.app.dto.AccountingCheckRowDto;
import com.bakouan.app.dto.AccountingKpiDto;
import com.bakouan.app.enums.OperatorType;

import java.time.LocalDate;
import java.util.List;

public interface AccountingService {
    List<AccountingCheckRowDto> check(LocalDate dateFrom, LocalDate dateTo);

    List<AccountingCheckRowDto> check(LocalDate dateFrom, LocalDate dateTo, OperatorType operator);

    AccountingKpiDto kpi(LocalDate dateFrom, LocalDate dateTo);

    AccountingKpiDto kpi(LocalDate dateFrom, LocalDate dateTo, OperatorType operator);

    LocalDate latestCarthagoDate(OperatorType operator);
}
