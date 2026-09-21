package com.bakouan.app.service.impl;

import com.bakouan.app.dto.CompensationDailyDto;
import com.bakouan.app.dto.CompensationPeriodResponseDto;
import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.MoovTransactionRepository;
import com.bakouan.app.repositories.OrangeTransactionRepository;
import com.bakouan.app.service.CompensationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CompensationServiceImpl implements CompensationService {

    private final BankTransactionRepository bankTransactionRepository;
    private final MoovTransactionRepository moovTransactionRepository;
    private final OrangeTransactionRepository orangeTransactionRepository;

    @Override
    public List<CompensationDailyDto> daily(LocalDate dateFrom, LocalDate dateTo, OperatorType operator) {
        LocalDate from = dateFrom == null ? LocalDate.now() : dateFrom;
        LocalDate to = dateTo == null ? from : dateTo;
        List<CompensationDailyDto> rows = new ArrayList<>();

        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (operator == null || operator == OperatorType.MOOV) {
                rows.add(buildForMoov(d));
            }
            if (operator == null || operator == OperatorType.ORANGE) {
                rows.add(buildForOrange(d));
            }
        }
        return rows;
    }

    @Override
    public CompensationPeriodResponseDto weekly(LocalDate dateFrom, LocalDate dateTo, OperatorType operator) {
        List<CompensationDailyDto> rows = daily(dateFrom, dateTo, operator);
        return aggregate("WEEK", dateFrom + " -> " + dateTo, rows);
    }

    @Override
    public CompensationPeriodResponseDto monthly(int year, int month, OperatorType operator) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<CompensationDailyDto> rows = daily(from, to, operator);
        return aggregate("MONTH", year + "-" + String.format("%02d", month), rows);
    }

    private CompensationDailyDto buildForMoov(LocalDate d) {
        LocalDateTime from = d.atStartOfDay();
        LocalDateTime to = d.plusDays(1).atStartOfDay();
        BankSuccessTotals bankTotals = bankSuccessTotals(from, to, OperatorType.MOOV);
        long opCount = moovTransactionRepository.countSuccessByTransactionDateRange(from, to);
        BigDecimal bank = bankTotals.amount();
        BigDecimal op = nz(moovTransactionRepository.sumSuccessAmountByTransactionDateRange(from, to));
        BigDecimal diff = bank.subtract(op);
        return new CompensationDailyDto(d, OperatorType.MOOV, opCount, bankTotals.count(), op, bank, diff, decision(diff), latestMoovClosingBalance(from, to));
    }

    private CompensationDailyDto buildForOrange(LocalDate d) {
        LocalDateTime from = d.atStartOfDay();
        LocalDateTime to = d.plusDays(1).atStartOfDay();
        BankSuccessTotals bankTotals = bankSuccessTotals(from, to, OperatorType.ORANGE);
        long opCount = orangeTransactionRepository.countSuccessByTransactionDateRange(from, to);
        BigDecimal bank = bankTotals.amount();
        BigDecimal op = nz(orangeTransactionRepository.sumSuccessAmountByTransactionDateRange(from, to));
        BigDecimal diff = bank.subtract(op);
        return new CompensationDailyDto(d, OperatorType.ORANGE, opCount, bankTotals.count(), op, bank, diff, decision(diff), null);
    }

    private String decision(BigDecimal diff) {
        return diff.compareTo(BigDecimal.ZERO) == 0 ? "OK_COMPENSATION" : "A_VERIFIER";
    }

    private CompensationPeriodResponseDto aggregate(String type, String label, List<CompensationDailyDto> rows) {
        BigDecimal totalOperator = rows.stream()
                .map(CompensationDailyDto::operatorSuccessAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        long totalOperatorCount = rows.stream()
                .mapToLong(CompensationDailyDto::operatorSuccessCount)
                .sum();
        BigDecimal totalBank = rows.stream()
                .map(CompensationDailyDto::bankSuccessAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        long totalBankCount = rows.stream()
                .mapToLong(CompensationDailyDto::bankSuccessCount)
                .sum();
        BigDecimal totalDiff = totalBank.subtract(totalOperator).setScale(2, RoundingMode.HALF_UP);
        BigDecimal moovClosingBalance = rows.stream()
                .filter(row -> row.operator() == OperatorType.MOOV)
                .map(CompensationDailyDto::moovClosingBalance)
                .filter(java.util.Objects::nonNull)
                .reduce((previous, current) -> current)
                .orElse(null);
        return new CompensationPeriodResponseDto(type, label, rows, totalOperatorCount, totalBankCount, totalOperator, totalBank, totalDiff, decision(totalDiff), moovClosingBalance);
    }

    private BigDecimal latestMoovClosingBalance(LocalDateTime from, LocalDateTime to) {
        return moovTransactionRepository.findClosingBalancesByCompletionTimeRange(from, to, org.springframework.data.domain.Pageable.ofSize(1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BankSuccessTotals bankSuccessTotals(LocalDateTime from, LocalDateTime to, OperatorType operator) {
        List<BankTransaction> rows = bankTransactionRepository.findByAllocationStatusAndTransactionDateRangeAndOperator(
                NormalizedBankStatus.SUCCESS_BANK,
                from,
                to,
                operator
        );
        List<BankTransaction> compensableRows = rows.stream()
                .filter(this::isCompensableBankSuccess)
                .toList();
        BigDecimal amount = compensableRows.stream()
                .map(BankTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BankSuccessTotals(compensableRows.size(), amount);
    }

    private boolean isCompensableBankSuccess(BankTransaction bank) {
        return bank != null
                && bank.getAllocationStatusNormalized() == NormalizedBankStatus.SUCCESS_BANK
                && !isGeneratedOrIssuedBankStatus(bank.getAllocationStatusRaw());
    }

    private boolean isGeneratedOrIssuedBankStatus(String raw) {
        String key = normalizeStatusKey(raw);
        return key.equals("paiementgenere")
                || key.equals("payementgenere")
                || key.equals("paymentissued")
                || key.equals("payementissued")
                || key.equals("paymentissuer")
                || key.equals("payementissuer");
    }

    private String normalizeStatusKey(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private record BankSuccessTotals(long count, BigDecimal amount) {
    }
}
