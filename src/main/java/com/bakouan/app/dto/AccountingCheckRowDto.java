package com.bakouan.app.dto;

import com.bakouan.app.enums.AccountingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountingCheckRowDto(
        Long bankTransactionId,
        String transactionId,
        LocalDate operationDate,
        BigDecimal amount,
        BigDecimal amplitudeCredit,
        String accountNumber,
        String bankPhoneNumber,
        String operationReference,
        String accountingDateRaw,
        String valueDateRaw,
        String pieceNumber,
        String eventNumber,
        String phoneNumber,
        String operationNature,
        AccountingStatus status
) {
}
