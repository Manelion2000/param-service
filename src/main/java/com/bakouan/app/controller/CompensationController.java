package com.bakouan.app.controller;

import com.bakouan.app.dto.CompensationDailyDto;
import com.bakouan.app.dto.CompensationDiscrepancyDto;
import com.bakouan.app.dto.CompensationPeriodResponseDto;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.OrangeTransaction;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.MoovTransactionRepository;
import com.bakouan.app.repositories.OrangeTransactionRepository;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import com.bakouan.app.service.CompensationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Year;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compensations")
@RequiredArgsConstructor
public class CompensationController {

    private final CompensationService compensationService;
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final MoovTransactionRepository moovTransactionRepository;
    private final OrangeTransactionRepository orangeTransactionRepository;

    @GetMapping("/daily")
    public List<CompensationDailyDto> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) OperatorType operator
    ) {
        return compensationService.daily(dateFrom, dateTo, operator);
    }

    @GetMapping("/weekly")
    public CompensationPeriodResponseDto weekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam OperatorType operator
    ) {
        return compensationService.weekly(dateFrom, dateTo, operator);
    }

    @GetMapping("/weekly/by-reference")
    public CompensationPeriodResponseDto weeklyByReference(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam OperatorType operator
    ) {
        LocalDate from = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate to = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return compensationService.weekly(from, to, operator);
    }

    @GetMapping("/monthly")
    public CompensationPeriodResponseDto monthly(
            @RequestParam int month,
            @RequestParam(required = false) Integer year,
            @RequestParam OperatorType operator
    ) {
        int y = year == null ? Year.now().getValue() : year;
        return compensationService.monthly(y, month, operator);
    }

    @GetMapping("/discrepancies")
    public Page<CompensationDiscrepancyDto> compensationDiscrepancies(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam OperatorType operator,
            Pageable pageable
    ) {
        Page<ReconciliationResult> page = reconciliationResultRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("run").get("operator"), operator),
                cb.greaterThanOrEqualTo(root.get("businessDate"), dateFrom),
                cb.lessThanOrEqualTo(root.get("businessDate"), dateTo),
                cb.notEqual(root.get("resultType"), ReconciliationResultType.MATCH_OK)
        ), pageable);

        Set<Long> bankIds = page.getContent().stream()
                .map(ReconciliationResult::getBankTransactionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> opIds = page.getContent().stream()
                .map(ReconciliationResult::getMoovTransactionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, BankTransaction> bankMap = bankTransactionRepository.findAllById(bankIds).stream()
                .collect(Collectors.toMap(BankTransaction::getId, b -> b));

        Map<Long, String> operatorPhoneMap;
        if (operator == OperatorType.MOOV) {
            operatorPhoneMap = moovTransactionRepository.findAllById(opIds).stream()
                    .collect(Collectors.toMap(MoovTransaction::getId, MoovTransaction::getMsisdn));
        } else {
            operatorPhoneMap = orangeTransactionRepository.findAllById(opIds).stream()
                    .collect(Collectors.toMap(OrangeTransaction::getId, OrangeTransaction::getSenderMobileNumber));
        }

        return page.map(r -> {
            BankTransaction bankTx = r.getBankTransactionId() == null ? null : bankMap.get(r.getBankTransactionId());
            String operatorPhone = r.getMoovTransactionId() == null ? null : operatorPhoneMap.get(r.getMoovTransactionId());
            return new CompensationDiscrepancyDto(
                    r.getBusinessDate(),
                    r.getTransactionKey(),
                    bankTx == null ? null : bankTx.getOperationReference(),
                    r.getResultType(),
                    operatorPhone,
                    r.getBankStatusRaw(),
                    r.getMoovStatusRaw(),
                    r.getBankAmount(),
                    r.getMoovAmount(),
                    r.getAmountDifference(),
                    r.getReason()
            );
        });
    }
}
