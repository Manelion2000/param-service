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
        Amounts amounts = computeAmounts(data.results);
        long totalBank = data.results.stream().filter(r -> r.getBankTransactionId() != null).count();
        long totalOperator = data.results.stream().filter(r -> r.getMoovTransactionId() != null).count();
        long totalResults = data.results.size();
        long matchOk = count(data.results, ReconciliationResultType.MATCH_OK);
        long debitATort = count(data.results, ReconciliationResultType.DEBIT_A_TORT);
        long creditSansDebit = count(data.results, ReconciliationResultType.CREDIT_SANS_DEBIT);
        long echecDesDeuxCotes = count(data.results, ReconciliationResultType.ECHEC_DES_DEUX_COTES);
        long absentCoteOperateur = count(data.results, operatorAbsentType(filter.channel()));
        long absentCoteBanque = count(data.results, ReconciliationResultType.ABSENT_COTE_BANQUE);
        long montantDifferent = count(data.results, ReconciliationResultType.MONTANT_DIFFERENT);
        long statutInconnu = count(data.results, ReconciliationResultType.STATUT_INCONNU);
        long doublons = count(data.results, ReconciliationResultType.DOUBLON_BANQUE) + count(data.results, ReconciliationResultType.DOUBLON_MOOV);
        long anomalies = totalResults - matchOk;
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
                rate(matchOk, totalResults),
                rate(anomalies, totalResults),
                amounts.bankTotal,
                amounts.operatorTotal,
                amounts.anomaliesTotal,
                amounts.ecartGlobal
        );
    }

    @Override
    public List<ResultDistributionDto> resultsDistribution(DashboardFilterRequest filter) {
        List<ReconciliationRun> runs = resolveRuns(filter);
        List<Long> runIds = runs.stream().map(ReconciliationRun::getId).toList();
        if (runIds.isEmpty()) {
            return List.of();
        }
        List<ResultTypeCountView> grouped = resultRepository.countByResultTypeForRuns(runIds, filter.effectiveFrom(), filter.effectiveTo());
        Map<DashboardResultTypeView, Long> distribution = new EnumMap<>(DashboardResultTypeView.class);
        for (ResultTypeCountView row : grouped) {
            DashboardResultTypeView view = toViewType(row.getResultType(), filter.channel());
            distribution.put(view, distribution.getOrDefault(view, 0L) + row.getCount());
        }
        return Arrays.stream(DashboardResultTypeView.values())
                .map(type -> new ResultDistributionDto(type, distribution.getOrDefault(type, 0L)))
                .filter(dto -> dto.count() > 0)
                .toList();
    }

    @Override
    public DashboardAmountsDto amounts(DashboardFilterRequest filter) {
        List<ReconciliationRun> runs = resolveRuns(filter);
        List<Long> runIds = runs.stream().map(ReconciliationRun::getId).toList();
        if (runIds.isEmpty()) {
            return new DashboardAmountsDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        AmountsView sums = resultRepository.sumsForRuns(runIds, filter.effectiveFrom(), filter.effectiveTo());
        List<ReconciliationResult> anomaliesRows = resultRepository.findByRunIdIn(runIds).stream()
                .filter(this::isAnomaly)
                .filter(r -> inDateRange(r.getBusinessDate(), filter.effectiveFrom(), filter.effectiveTo()))
                .toList();
        BigDecimal anomalies = anomaliesRows.stream().map(this::anomalyAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bank = sums == null || sums.getBankTotal() == null ? BigDecimal.ZERO : sums.getBankTotal();
        BigDecimal operator = sums == null || sums.getOperatorTotal() == null ? BigDecimal.ZERO : sums.getOperatorTotal();
        return new DashboardAmountsDto(
                bank,
                operator,
                anomalies,
                bank.subtract(operator)
        );
    }

    @Override
    public List<DashboardTimelinePointDto> timeline(DashboardFilterRequest filter) {
        FilteredData data = loadFilteredData(filter);
        Map<Integer, Long> txByHour = new HashMap<>();
        Map<Integer, Long> anomalyByHour = new HashMap<>();
        Set<String> anomalyKeys = data.results.stream()
                .filter(r -> r.getResultType() != ReconciliationResultType.MATCH_OK)
                .map(ReconciliationResult::getTransactionKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<BankTransaction> bankRows = bankTransactionRepository.findByFileImportIdIn(data.bankImportIds);
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
            List<MoovTransaction> rows = moovTransactionRepository.findByFileImportIdIn(data.operatorImportIds).stream()
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
            List<OrangeTransaction> rows = orangeTransactionRepository.findByFileImportIdIn(data.operatorImportIds).stream()
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
        List<ReconciliationRun> runs = resolveRuns(filter);
        List<Long> runIds = runs.stream().map(ReconciliationRun::getId).toList();
        if (runIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<ReconciliationResult> results = resultRepository.findTopAnomaliesForRuns(runIds, filter.effectiveFrom(), filter.effectiveTo(), pageable);
        List<TopAnomalyDto> page = results.getContent().stream()
                .map(this::toTopAnomalyDto)
                .toList();
        return new PageImpl<>(page, pageable, results.getTotalElements());
    }

    @Override
    public DataQualityDto dataQuality(DashboardFilterRequest filter) {
        FilteredData data = loadFilteredData(filter);
        long totalImports = data.imports.size();
        long validRows = data.validRows;
        long invalidRows = data.invalidRows;
        long duplicateCount = count(data.results, ReconciliationResultType.DOUBLON_BANQUE)
                + count(data.results, ReconciliationResultType.DOUBLON_MOOV);
        long parsedRows = validRows + invalidRows;
        return new DataQualityDto(
                totalImports,
                invalidRows,
                validRows,
                rate(validRows, parsedRows),
                duplicateCount,
                rate(duplicateCount, validRows),
                rate(invalidRows, parsedRows)
        );
    }

    private FilteredData loadFilteredData(DashboardFilterRequest filter) {
        List<ReconciliationRun> runs = resolveRuns(filter);
        List<Long> runIds = runs.stream().map(ReconciliationRun::getId).toList();
        List<ReconciliationResult> rows = runIds.isEmpty() ? List.of() : resultRepository.findByRunIdIn(runIds);
        LocalDate from = filter.effectiveFrom();
        LocalDate to = filter.effectiveTo();
        if (from != null && to != null) {
            rows = rows.stream()
                    .filter(r -> r.getBusinessDate() != null)
                    .filter(r -> !r.getBusinessDate().isBefore(from) && !r.getBusinessDate().isAfter(to))
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
        LocalDate from = filter.effectiveFrom();
        LocalDate to = filter.effectiveTo();
        List<ReconciliationRun> runs;
        if (from != null && to != null) {
            runs = runRepository.findByOperatorAndBusinessDateOverlap(filter.channel(), from, to, Pageable.unpaged()).getContent();
        } else {
            runs = runRepository.findByOperator(filter.channel(), Pageable.unpaged()).getContent();
        }
        if (filter.importId() == null) {
            return runs;
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
        LocalDate from = filter.effectiveFrom();
        LocalDate to = filter.effectiveTo();
        SourceType operatorSource = filter.channel() == OperatorType.MOOV ? SourceType.MOOV : SourceType.ORANGE;
        List<FileImport> bankImports = from != null && to != null
                ? fileImportRepository.findBySourceTypeAndOperatorScopeAndBusinessDateBetween(SourceType.BANQUE, filter.channel(), from, to)
                : fileImportRepository.findBySourceTypeAndOperatorScope(SourceType.BANQUE, filter.channel());
        List<FileImport> operatorImports = from != null && to != null
                ? fileImportRepository.findBySourceTypeAndBusinessDateBetween(operatorSource, from, to)
                : fileImportRepository.findBySourceType(operatorSource);

        Set<Long> runImportIds = new HashSet<>();
        for (ReconciliationRun run : runs) {
            runImportIds.addAll(parseCsvIds(run.getBankImportIds()));
            runImportIds.addAll(parseCsvIds(filter.channel() == OperatorType.MOOV ? run.getMoovImportIds() : run.getOrangeImportIds()));
        }
        return java.util.stream.Stream.concat(bankImports.stream(), operatorImports.stream())
                .filter(f -> runImportIds.contains(f.getId()))
                .toList();
    }

    private Amounts computeAmounts(List<ReconciliationResult> rows) {
        BigDecimal bankTotal = rows.stream().map(ReconciliationResult::getBankAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal operatorTotal = rows.stream().map(ReconciliationResult::getMoovAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
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
        return row.getResultType() != ReconciliationResultType.MATCH_OK;
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

    private record Amounts(BigDecimal bankTotal, BigDecimal operatorTotal, BigDecimal anomaliesTotal, BigDecimal ecartGlobal) {
    }
}
