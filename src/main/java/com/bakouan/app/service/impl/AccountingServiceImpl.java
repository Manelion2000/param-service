package com.bakouan.app.service.impl;

import com.bakouan.app.dto.AccountingCheckRowDto;
import com.bakouan.app.dto.AccountingKpiDto;
import com.bakouan.app.enums.AccountingStatus;
import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.model.AmplitudeTransaction;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.repositories.AmplitudeTransactionRepository;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.service.AccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountingServiceImpl implements AccountingService {

    private final BankTransactionRepository bankTransactionRepository;
    private final AmplitudeTransactionRepository amplitudeTransactionRepository;

    @Override
    public List<AccountingCheckRowDto> check(LocalDate dateFrom, LocalDate dateTo) {
        return check(dateFrom, dateTo, null);
    }

    @Override
    public List<AccountingCheckRowDto> check(LocalDate dateFrom, LocalDate dateTo, OperatorType operator) {
        LocalDateTime bankFrom = dateFrom.atStartOfDay();
        LocalDateTime bankTo = dateTo.plusDays(1).atStartOfDay();
        List<BankTransaction> banks = accountingCandidateBanks(bankFrom, bankTo, operator);
        List<AmplitudeTransaction> amplitudes = amplitudeTransactionRepository.findAll();
        List<AmplitudeTransaction> periodAmplitudes = amplitudeTransactionRepository
                .findByOperationDateGreaterThanEqualAndOperationDateLessThan(bankFrom, bankTo);
        Map<String, List<AmplitudeTransaction>> amplitudeByRef = groupAmplitudeByReference(amplitudes);

        List<AccountingCheckRowDto> out = new ArrayList<>();
        Set<Long> matchedAmplitudeIds = new java.util.HashSet<>();
        for (BankTransaction bank : banks) {
            AmplitudeTransaction matched = findMatchedAmplitude(bank, amplitudeByRef, amplitudes);
            if (isGeneratedPayment(bank) && matched == null) {
                continue;
            }
            if (matched != null && matched.getId() != null) {
                matchedAmplitudeIds.add(matched.getId());
            }
            AccountingStatus status = matched == null ? AccountingStatus.NON_COMPTABILISE : AccountingStatus.COMPTABILISE;
            out.add(new AccountingCheckRowDto(
                    bank.getId(),
                    bank.getTransactionId(),
                    bank.getTransactionDate() == null ? null : bank.getTransactionDate().toLocalDate(),
                    bank.getAmount(),
                    matched == null ? null : matched.getAmount(),
                    bank.getAccountNumber(),
                    bank.getPhoneNumber(),
                    bank.getOperationReference(),
                    matched == null ? null : matched.getAccountingDateRaw(),
                    matched == null ? null : matched.getValueDateRaw(),
                    matched == null ? null : matched.getPieceNumber(),
                    matched == null ? null : matched.getEventNumber(),
                    matched == null ? null : matched.getPhoneNumber(),
                    normalizeOperationNature(bank.getOperationNature()),
                    status
            ));
        }
        for (AmplitudeTransaction amplitude : periodAmplitudes) {
            if (amplitude.getId() != null && matchedAmplitudeIds.contains(amplitude.getId())) {
                continue;
            }
            out.add(new AccountingCheckRowDto(
                    null,
                    null,
                    amplitude.getOperationDate() == null ? null : amplitude.getOperationDate().toLocalDate(),
                    null,
                    amplitude.getAmount(),
                    amplitude.getAccountNumber(),
                    null,
                    amplitude.getOperationReference(),
                    amplitude.getAccountingDateRaw(),
                    amplitude.getValueDateRaw(),
                    amplitude.getPieceNumber(),
                    amplitude.getEventNumber(),
                    amplitude.getPhoneNumber(),
                    normalizeOperationNature(amplitude.getDirection()),
                    AccountingStatus.AMPLITUDE_SANS_CARTHAGO
            ));
        }
        return out;
    }

    @Override
    public AccountingKpiDto kpi(LocalDate dateFrom, LocalDate dateTo) {
        return kpi(dateFrom, dateTo, null);
    }

    @Override
    public AccountingKpiDto kpi(LocalDate dateFrom, LocalDate dateTo, OperatorType operator) {
        LocalDateTime bankFrom = dateFrom.atStartOfDay();
        LocalDateTime bankTo = dateTo.plusDays(1).atStartOfDay();
        List<BankTransaction> banks = accountingCandidateBanks(bankFrom, bankTo, operator);
        List<AmplitudeTransaction> amplitudes = amplitudeTransactionRepository.findAll();
        Map<String, List<AmplitudeTransaction>> amplitudeByRef = groupAmplitudeByReference(amplitudes);

        long totalTransactions = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        long comptabilizedTransactions = 0;
        BigDecimal comptabilizedAmount = BigDecimal.ZERO;
        long nonComptabilizedTransactions = 0;
        BigDecimal nonComptabilizedAmount = BigDecimal.ZERO;
        BigDecimal netGapAmount = BigDecimal.ZERO;
        BigDecimal absoluteGapAmount = BigDecimal.ZERO;
        long transactionsWithGapCount = 0;
        BigDecimal totalGapAmount = BigDecimal.ZERO;

        for (BankTransaction bank : banks) {
            AmplitudeTransaction matched = findMatchedAmplitude(bank, amplitudeByRef, amplitudes);
            if (isGeneratedPayment(bank) && matched == null) {
                continue;
            }
            BigDecimal bankAmount = bank.getAmount() == null ? BigDecimal.ZERO : bank.getAmount();
            totalTransactions++;
            totalAmount = totalAmount.add(bankAmount);
            if (matched != null) {
                comptabilizedTransactions++;
                comptabilizedAmount = comptabilizedAmount.add(bankAmount);
                BigDecimal ampAmount = matched.getAmount() == null ? BigDecimal.ZERO : matched.getAmount();
                BigDecimal gap = bankAmount.subtract(ampAmount);
                netGapAmount = netGapAmount.add(gap);
                BigDecimal abs = gap.abs();
                absoluteGapAmount = absoluteGapAmount.add(abs);
                if (abs.compareTo(BigDecimal.ZERO) > 0) {
                    transactionsWithGapCount++;
                    totalGapAmount = totalGapAmount.add(abs);
                }
            } else {
                nonComptabilizedTransactions++;
                nonComptabilizedAmount = nonComptabilizedAmount.add(bankAmount);
            }
        }

        return new AccountingKpiDto(
                totalTransactions,
                totalAmount,
                comptabilizedTransactions,
                comptabilizedAmount,
                rate(comptabilizedTransactions, totalTransactions),
                rate(comptabilizedAmount, totalAmount),
                nonComptabilizedTransactions,
                nonComptabilizedAmount,
                rate(nonComptabilizedTransactions, totalTransactions),
                nonComptabilizedAmount,
                netGapAmount,
                absoluteGapAmount,
                transactionsWithGapCount,
                totalGapAmount
        );
    }

    @Override
    public LocalDate latestCarthagoDate(OperatorType operator) {
        LocalDateTime latest = bankTransactionRepository.findLatestSuccessTransactionDateByOperator(operator);
        return latest == null ? null : latest.toLocalDate();
    }

    private Map<String, List<AmplitudeTransaction>> groupAmplitudeByReference(List<AmplitudeTransaction> amplitudes) {
        return amplitudes.stream()
                .filter(a -> normalizeReference(a.getOperationReference()) != null)
                .collect(Collectors.groupingBy(a -> normalizeReference(a.getOperationReference())));
    }

    private List<BankTransaction> accountingCandidateBanks(LocalDateTime bankFrom, LocalDateTime bankTo, OperatorType operator) {
        if (operator != null) {
            return bankTransactionRepository.findByAllocationStatusAndTransactionDateRangeAndOperator(
                    NormalizedBankStatus.SUCCESS_BANK,
                    bankFrom,
                    bankTo,
                    operator
            );
        }
        return bankTransactionRepository
                .findByAllocationStatusNormalizedAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(
                        NormalizedBankStatus.SUCCESS_BANK,
                        bankFrom,
                        bankTo
                );
    }

    private AmplitudeTransaction findMatchedAmplitude(BankTransaction bank,
                                                      Map<String, List<AmplitudeTransaction>> amplitudeByRef,
                                                      List<AmplitudeTransaction> amplitudes) {
        String ref = normalizeReference(bank.getOperationReference());
        if (ref != null && !ref.isBlank()) {
            List<AmplitudeTransaction> byRef = amplitudeByRef.get(ref);
            if (byRef != null && !byRef.isEmpty()) {
                return byRef.get(0);
            }
        }
        return amplitudes.stream()
                .filter(a -> a.getOperationDate() != null && a.getAmount() != null)
                .filter(a -> bank.getTransactionDate() != null && bank.getAmount() != null)
                .filter(a -> Math.abs(ChronoUnit.DAYS.between(bank.getTransactionDate().toLocalDate(), a.getOperationDate().toLocalDate())) <= 1)
                .filter(a -> bank.getAmount().compareTo(a.getAmount()) == 0)
                .filter(a -> {
                    String bankAccount = normalizeDigits(bank.getAccountNumber());
                    String ampAccount = normalizeDigits(a.getAccountNumber());
                    return bankAccount == null || ampAccount == null || bankAccount.equals(ampAccount);
                })
                .findFirst()
                .orElse(null);
    }

    private boolean isGeneratedPayment(BankTransaction bank) {
        if (bank == null || bank.getAllocationStatusRaw() == null) {
            return false;
        }
        String normalized = Normalizer.normalize(bank.getAllocationStatusRaw(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return normalized.equals("paiement genere")
                || normalized.equals("payment generated")
                || normalized.equals("payment genere");
    }

    private String normalizeReference(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) return null;
        return digits.length() < 10 ? null : digits;
    }

    private String normalizeDigits(String value) {
        if (value == null) return null;
        String v = value.replaceAll("\\D", "");
        return v.isBlank() ? null : v;
    }

    private String normalizeOperationNature(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (normalized.contains("BANK_TO_WALLET") || normalized.contains("BANK_TO_MOOV")
                || normalized.contains("BANQUE_TO_WALLET") || normalized.contains("BANQUE_VERS_WALLET")
                || normalized.contains("BANQUE_WALLET")) {
            return "BANK_TO_WALLET";
        }
        if (normalized.contains("WALLET_TO_BANK") || normalized.contains("MOOV_TO_BANK")
                || normalized.contains("WALLET_VERS_BANQUE") || normalized.contains("WALLET_BANQUE")) {
            return "WALLET_TO_BANK";
        }
        return normalized;
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return Objects.requireNonNullElse(numerator, BigDecimal.ZERO).multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
