package com.bakouan.app.service.impl;

import com.bakouan.app.dto.dashboard.*;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:00");

    private final ReconciliationRunRepository runRepository;
    private final ReconciliationResultRepository resultRepository;
    private final FileImportRepository fileImportRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final MoovTransactionRepository moovTransactionRepository;
    private final OrangeTransactionRepository orangeTransactionRepository;

    @Override
    public DashboardSummaryDto summary(DashboardFilterRequest filter) {
        FilteredData data = loadFilteredData(filter);
        List<ReconciliationResult> financialRows = data.results.stream().filter(this::isFinanciallyRelevant).toList();
        Amounts amounts = computeAmounts(financialRows);
        long totalBank = financialRows.stream().filter(r -> r.getBankTransactionId() != null).count();
        long totalOperator = financialRows.stream().filter(r -> r.getMoovTransactionId() != null).count();
        long totalResults = data.results.size();
        long financialTotal = financialRows.size();
        long matchOk = count(financialRows, ReconciliationResultType.MATCH_OK);
        long debitATort = count(financialRows, ReconciliationResultType.DEBIT_A_TORT);
        long creditSansDebit = count(financialRows, ReconciliationResultType.CREDIT_SANS_DEBIT);
        long echecDesDeuxCotes = count(financialRows, ReconciliationResultType.ECHEC_DES_DEUX_COTES);
        long absentCoteOperateur = count(financialRows, operatorAbsentType(filter.channel()));
        long absentCoteBanque = count(financialRows, ReconciliationResultType.ABSENT_COTE_BANQUE);
        long montantDifferent = count(financialRows, ReconciliationResultType.MONTANT_DIFFERENT);
        long statutInconnu = count(financialRows, ReconciliationResultType.STATUT_INCONNU);
        long doublons = count(financialRows, ReconciliationResultType.DOUBLON_BANQUE) + count(financialRows, ReconciliationResultType.DOUBLON_MOOV);
        long anomalies = financialTotal - matchOk;
        long invalidRows = data.invalidRows;

        return new DashboardSummaryDto(
                filter.periodType(),
                filter.periodType() == DashboardPeriodType.DAY ? filter.businessDate() : null,
                filter.periodType() == DashboardPeriodType.RANGE ? filter.effectiveFrom() : null,
                filter.periodType() == DashboardPeriodType.RANGE ? filter.effectiveTo() : null,
                filter.channel(),
                totalBank,
                totalOperator,
                totalResults,
                matchOk,
                debitATort,
                creditSansDebit,
                echecDesDeuxCotes,
                absentCoteOperateur,
                absentCoteBanque,
                montantDifferent,
                statutInconnu,
                doublons,
                invalidRows,
                rate(matchOk, totalBank),
                rate(matchOk, financialTotal),
                rate(anomalies, financialTotal),
                amounts.bankTotal,
                amounts.operatorTotal,
                amounts.anomaliesTotal,
                amounts.ecartGlobal
        );
    }

    @Override
    public List<ResultDistributionDto> resultsDistribution(DashboardFilterRequest filter) {
        FilteredData data = loadFilteredData(filter);
        Map<DashboardResultTypeView, Long> distribution = new EnumMap<>(DashboardResultTypeView.class);
        for (ReconciliationResult row : data.results) {
            DashboardResultTypeView view = toViewType(row.getResultType(), filter.channel());
            distribution.put(view, distribution.getOrDefault(view, 0L) + 1L);
        }
        return Arrays.stream(DashboardResultTypeView.values())
                .map(type -> new ResultDistributionDto(type, distribution.getOrDefault(type, 0L)))
                .filter(dto -> dto.count() > 0)
                .toList();
    }

    @Override
    public DashboardAmountsDto amounts(DashboardFilterRequest filter) {
        Amounts amounts = computeAmounts(loadFilteredData(filter).results.stream().filter(this::isFinanciallyRelevant).toList());
        return new DashboardAmountsDto(
                amounts.bankTotal,
                amounts.operatorTotal,
                amounts.anomaliesTotal,
                amounts.ecartGlobal
        );
    }

    @Override
    public List<DashboardTimelinePointDto> timeline(DashboardFilterRequest filter) {
        FilteredData data = loadFilteredData(filter);
        Map<Integer, Long> txByHour = new HashMap<>();
        Map<Integer, Long> anomalyByHour = new HashMap<>();
        Set<String> anomalyKeys = data.results.stream()
                .filter(r -> r.getResultType() != ReconciliationResultType.MATCH_OK)
                .filter(this::isFinanciallyRelevant)
                .map(ReconciliationResult::getTransactionKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> bankTransactionIds = data.results.stream()
                .map(ReconciliationResult::getBankTransactionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<BankTransaction> bankRows = bankTransactionIds.isEmpty() ? List.of() : bankTransactionRepository.findAllById(bankTransactionIds);
        for (BankTransaction tx : bankRows) {
            if (tx.getTransactionDate() == null) {
                continue;
            }
            int hour = tx.getTransactionDate().getHour();
            txByHour.put(hour, txByHour.getOrDefault(hour, 0L) + 1L);
            if (anomalyKeys.contains(tx.getTransactionId())) {
                anomalyByHour.put(hour, anomalyByHour.getOrDefault(hour, 0L) + 1L);
            }
        }

        if (filter.channel() == OperatorType.MOOV) {
            Set<Long> operatorTransactionIds = data.results.stream()
                    .map(ReconciliationResult::getMoovTransactionId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            List<MoovTransaction> rows = operatorTransactionIds.isEmpty() ? List.of() : moovTransactionRepository.findAllById(operatorTransactionIds).stream()
                    .filter(r -> r.getCompletionTime() != null || r.getInitiationTime() != null)
                    .toList();
            for (MoovTransaction tx : rows) {
                LocalDateTime dateTime = tx.getCompletionTime() != null ? tx.getCompletionTime() : tx.getInitiationTime();
                int hour = dateTime.getHour();
                txByHour.put(hour, txByHour.getOrDefault(hour, 0L) + 1L);
                if (anomalyKeys.contains(tx.getReceiptNo())) {
                    anomalyByHour.put(hour, anomalyByHour.getOrDefault(hour, 0L) + 1L);
                }
            }
        } else {
            Set<Long> operatorTransactionIds = data.results.stream()
                    .map(ReconciliationResult::getMoovTransactionId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            List<OrangeTransaction> rows = operatorTransactionIds.isEmpty() ? List.of() : orangeTransactionRepository.findAllById(operatorTransactionIds).stream()
                    .filter(r -> r.getTransactionDateTime() != null)
                    .toList();
            for (OrangeTransaction tx : rows) {
                int hour = tx.getTransactionDateTime().getHour();
                txByHour.put(hour, txByHour.getOrDefault(hour, 0L) + 1L);
                if (anomalyKeys.contains(tx.getOmTransactionId())) {
                    anomalyByHour.put(hour, anomalyByHour.getOrDefault(hour, 0L) + 1L);
                }
            }
        }

        return build24Hours(txByHour, anomalyByHour);
    }

    @Override
    public Page<TopAnomalyDto> topAnomalies(DashboardFilterRequest filter, Pageable pageable) {
        List<TopAnomalyDto> anomalies = loadFilteredData(filter).results.stream()
                .filter(this::isAnomaly)
                .sorted(Comparator.comparing(this::anomalyAmount).reversed())
                .map(this::toTopAnomalyDto)
                .toList();
        if (anomalies.isEmpty()) {
            return Page.empty(pageable);
        }
        int start = Math.toIntExact(Math.min(pageable.getOffset(), anomalies.size()));
        int end = Math.min(start + pageable.getPageSize(), anomalies.size());
        return new PageImpl<>(anomalies.subList(start, end), pageable, anomalies.size());
    }

    @Override
    public DataQualityDto dataQuality(DashboardFilterRequest filter) {
        FilteredData data = loadFilteredData(filter);
        long totalImports = data.imports.size();
        long validRows = data.validRows;
        long invalidRows = data.invalidRows;
        long duplicateCount = count(data.results, ReconciliationResultType.DOUBLON_BANQUE)
                + count(data.results, ReconciliationResultType.DOUBLON_MOOV);
        long operatorOutOfScopeCount = count(
                data.results,
                ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE
        );
        long parsedRows = validRows + invalidRows;
        return new DataQualityDto(
                totalImports,
                invalidRows,
                validRows,
                rate(validRows, parsedRows),
                duplicateCount,
                rate(duplicateCount, validRows),
                rate(invalidRows, parsedRows),
                operatorOutOfScopeCount,
                rate(operatorOutOfScopeCount, data.results.size())
        );
    }

    private FilteredData loadFilteredData(DashboardFilterRequest filter) {
        List<ReconciliationRun> runs = resolveRuns(filter);
        List<Long> runIds = runs.stream().map(ReconciliationRun::getId).toList();
        List<ReconciliationResult> rows = runIds.isEmpty() ? List.of() : resultRepository.findByRunIdIn(runIds);
        LocalDate from = filter.effectiveFrom();
        LocalDate to = filter.effectiveTo();
        if (from != null && to != null) {
            TransactionDateLookup lookup = buildTransactionDateLookup(rows, filter.channel());
            rows = rows.stream()
                    .filter(r -> inDateRange(resolveTransactionDate(r, lookup), from, to))
                    .toList();
        }

        List<FileImport> imports = resolveImports(filter, runs);
        long invalidRows = imports.stream().map(FileImport::getInvalidRows).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long validRows = imports.stream().map(FileImport::getValidRows).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();

        Set<Long> bankImportIds = new HashSet<>();
        Set<Long> operatorImportIds = new HashSet<>();
        for (ReconciliationRun run : runs) {
            bankImportIds.addAll(parseCsvIds(run.getBankImportIds()));
            if (filter.channel() == OperatorType.MOOV) {
                operatorImportIds.addAll(parseCsvIds(run.getMoovImportIds()));
            } else {
                operatorImportIds.addAll(parseCsvIds(run.getOrangeImportIds()));
            }
        }
        if (filter.importId() != null) {
            bankImportIds.retainAll(Set.of(filter.importId()));
            operatorImportIds.retainAll(Set.of(filter.importId()));
        }
        return new FilteredData(rows, imports, invalidRows, validRows, bankImportIds, operatorImportIds);
    }

    private List<ReconciliationRun> resolveRuns(DashboardFilterRequest filter) {
        if (filter.runId() != null) {
            return runRepository.findById(filter.runId())
                    .filter(run -> run.getOperator() == filter.channel())
                    .map(List::of)
                    .orElse(List.of());
        }
        List<ReconciliationRun> runs;
        LocalDate from = filter.effectiveFrom();
        LocalDate to = filter.effectiveTo();
        if (from != null && to != null) {
            runs = runRepository.findByOperatorAndBusinessDateOverlap(filter.channel(), from, to, Pageable.unpaged()).getContent();
        } else {
            runs = runRepository.findByOperator(filter.channel(), Pageable.unpaged()).getContent();
        }
        if (filter.importId() == null) {
            return runs.stream()
                    .max(Comparator.comparing(ReconciliationRun::getStartedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .map(List::of)
                    .orElse(List.of());
        }
        return runs.stream()
                .filter(run -> parseCsvIds(run.getBankImportIds()).contains(filter.importId())
                        || parseCsvIds(filter.channel() == OperatorType.MOOV ? run.getMoovImportIds() : run.getOrangeImportIds()).contains(filter.importId()))
                .toList();
    }

    private List<FileImport> resolveImports(DashboardFilterRequest filter, List<ReconciliationRun> runs) {
        if (filter.importId() != null) {
            return fileImportRepository.findById(filter.importId()).map(List::of).orElse(List.of());
        }
        Set<Long> runImportIds = new HashSet<>();
        for (ReconciliationRun run : runs) {
            runImportIds.addAll(parseCsvIds(run.getBankImportIds()));
            runImportIds.addAll(parseCsvIds(filter.channel() == OperatorType.MOOV ? run.getMoovImportIds() : run.getOrangeImportIds()));
        }
        return runImportIds.isEmpty() ? List.of() : fileImportRepository.findAllById(runImportIds);
    }

    private TransactionDateLookup buildTransactionDateLookup(List<ReconciliationResult> rows, OperatorType channel) {
        Set<Long> bankTransactionIds = rows.stream()
                .map(ReconciliationResult::getBankTransactionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> operatorTransactionIds = rows.stream()
                .map(ReconciliationResult::getMoovTransactionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, LocalDate> bankDates = bankTransactionIds.isEmpty() ? Map.of() : bankTransactionRepository.findAllById(bankTransactionIds).stream()
                .filter(tx -> tx.getTransactionDate() != null)
                .collect(Collectors.toMap(BankTransaction::getId, tx -> tx.getTransactionDate().toLocalDate(), (left, right) -> left));
        Map<Long, LocalDate> operatorDates = operatorTransactionIds.isEmpty() ? Map.of() : loadOperatorTransactionDates(operatorTransactionIds, channel);
        return new TransactionDateLookup(bankDates, operatorDates);
    }

    private Map<Long, LocalDate> loadOperatorTransactionDates(Set<Long> transactionIds, OperatorType channel) {
        if (channel == OperatorType.MOOV) {
            return moovTransactionRepository.findAllById(transactionIds).stream()
                    .filter(tx -> tx.getCompletionTime() != null || tx.getInitiationTime() != null)
                    .collect(Collectors.toMap(
                            MoovTransaction::getId,
                            tx -> (tx.getCompletionTime() != null ? tx.getCompletionTime() : tx.getInitiationTime()).toLocalDate(),
                            (left, right) -> left
                    ));
        }
        return orangeTransactionRepository.findAllById(transactionIds).stream()
                .filter(tx -> tx.getTransactionDateTime() != null)
                .collect(Collectors.toMap(OrangeTransaction::getId, tx -> tx.getTransactionDateTime().toLocalDate(), (left, right) -> left));
    }

    private LocalDate resolveTransactionDate(ReconciliationResult row, TransactionDateLookup lookup) {
        if (row.getBankTransactionId() != null) {
            LocalDate bankDate = lookup.bankDates().get(row.getBankTransactionId());
            if (bankDate != null) {
                return bankDate;
            }
        }
        if (row.getMoovTransactionId() != null) {
            LocalDate operatorDate = lookup.operatorDates().get(row.getMoovTransactionId());
            if (operatorDate != null) {
                return operatorDate;
            }
        }
        return row.getBusinessDate();
    }

    private Amounts computeAmounts(List<ReconciliationResult> rows) {
        List<ReconciliationResult> completedRows = rows.stream()
                .filter(this::isCompletedOnBothSides)
                .toList();
        BigDecimal bankTotal = completedRows.stream().map(ReconciliationResult::getBankAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal operatorTotal = completedRows.stream().map(ReconciliationResult::getMoovAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal anomaliesTotal = rows.stream()
                .filter(this::isAnomaly)
                .map(this::anomalyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Amounts(bankTotal, operatorTotal, anomaliesTotal, bankTotal.subtract(operatorTotal));
    }

    private BigDecimal anomalyAmount(ReconciliationResult row) {
        if (row.getAmountDifference() != null) {
            return row.getAmountDifference().abs();
        }
        if (row.getBankAmount() != null && row.getMoovAmount() != null) {
            return row.getBankAmount().subtract(row.getMoovAmount()).abs();
        }
        if (row.getBankAmount() != null) {
            return row.getBankAmount().abs();
        }
        if (row.getMoovAmount() != null) {
            return row.getMoovAmount().abs();
        }
        return BigDecimal.ZERO;
    }

    private List<DashboardTimelinePointDto> build24Hours(Map<Integer, Long> txByHour, Map<Integer, Long> anomalyByHour) {
        List<DashboardTimelinePointDto> points = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            String label = LocalDate.now().atTime(hour, 0).format(HOUR_FORMAT);
            points.add(new DashboardTimelinePointDto(
                    label,
                    txByHour.getOrDefault(hour, 0L),
                    anomalyByHour.getOrDefault(hour, 0L)
            ));
        }
        return points;
    }

    private DashboardResultTypeView toViewType(ReconciliationResultType type, OperatorType channel) {
        return switch (type) {
            case MATCH_OK -> DashboardResultTypeView.MATCH_OK;
            case DEBIT_A_TORT -> DashboardResultTypeView.DEBIT_A_TORT;
            case CREDIT_SANS_DEBIT -> DashboardResultTypeView.CREDIT_SANS_DEBIT;
            case ECHEC_DES_DEUX_COTES -> DashboardResultTypeView.ECHEC_DES_DEUX_COTES;
            case ABSENT_COTE_BANQUE -> DashboardResultTypeView.ABSENT_COTE_BANQUE;
            case ABSENT_COTE_MOOV, ABSENT_COTE_ORANGE -> DashboardResultTypeView.ABSENT_COTE_OPERATEUR;
            case OPERATEUR_NON_ABOUTI_SANS_BANQUE -> DashboardResultTypeView.OPERATEUR_NON_ABOUTI_SANS_BANQUE;
            case APPROVISIONNEMENT -> DashboardResultTypeView.APPROVISIONNEMENT;
            case MONTANT_DIFFERENT -> DashboardResultTypeView.MONTANT_DIFFERENT;
            case STATUT_INCONNU -> DashboardResultTypeView.STATUT_INCONNU;
            case DOUBLON_BANQUE, DOUBLON_MOOV -> DashboardResultTypeView.DOUBLONS;
        };
    }

    private TopAnomalyDto toTopAnomalyDto(ReconciliationResult row) {
        return new TopAnomalyDto(
                row.getTransactionKey(),
                row.getBusinessDate(),
                toViewType(row.getResultType(), null),
                row.getBankStatusRaw(),
                row.getMoovStatusRaw(),
                row.getBankAmount(),
                row.getMoovAmount(),
                row.getAmountDifference(),
                row.getReason()
        );
    }

    private boolean isAnomaly(ReconciliationResult row) {
        return isFinanciallyRelevant(row) && row.getResultType() != ReconciliationResultType.MATCH_OK;
    }

    private boolean isFinanciallyRelevant(ReconciliationResult row) {
        return row.getResultType() != ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE
                && row.getResultType() != ReconciliationResultType.APPROVISIONNEMENT;
    }

    private boolean isCompletedOnBothSides(ReconciliationResult row) {
        return row.getBankTransactionId() != null
                && row.getMoovTransactionId() != null
                && row.getBankAmount() != null
                && row.getMoovAmount() != null
                && (row.getResultType() == ReconciliationResultType.MATCH_OK
                || row.getResultType() == ReconciliationResultType.MONTANT_DIFFERENT);
    }

    private ReconciliationResultType operatorAbsentType(OperatorType channel) {
        return channel == OperatorType.MOOV ? ReconciliationResultType.ABSENT_COTE_MOOV : ReconciliationResultType.ABSENT_COTE_ORANGE;
    }

    private long count(List<ReconciliationResult> rows, ReconciliationResultType type) {
        return rows.stream().filter(r -> r.getResultType() == type).count();
    }

    private BigDecimal rate(long count, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private boolean inDateRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return from == null && to == null;
        }
        if (from != null && date.isBefore(from)) {
            return false;
        }
        if (to != null && date.isAfter(to)) {
            return false;
        }
        return true;
    }

    private Set<Long> parseCsvIds(String csvIds) {
        if (csvIds == null || csvIds.isBlank()) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        for (String token : csvIds.split(",")) {
            try {
                ids.add(Long.parseLong(token.trim()));
            } catch (NumberFormatException ignored) {
                // skip malformed token
            }
        }
        return ids;
    }

    private record FilteredData(
            List<ReconciliationResult> results,
            List<FileImport> imports,
            long invalidRows,
            long validRows,
            Set<Long> bankImportIds,
            Set<Long> operatorImportIds
    ) {
    }

    private record TransactionDateLookup(Map<Long, LocalDate> bankDates, Map<Long, LocalDate> operatorDates) {
    }

    private record Amounts(BigDecimal bankTotal, BigDecimal operatorTotal, BigDecimal anomaliesTotal, BigDecimal ecartGlobal) {
    }
}
