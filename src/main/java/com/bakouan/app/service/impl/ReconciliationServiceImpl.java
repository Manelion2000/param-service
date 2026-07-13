package com.bakouan.app.service.impl;

import com.bakouan.app.dto.ReconciliationRunRequest;
import com.bakouan.app.dto.ReconciliationSummaryDto;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.enums.ReconciliationRunStatus;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.FileImportRepository;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import com.bakouan.app.repositories.ReconciliationRunRepository;
import com.bakouan.app.service.ReconciliationService;
import com.bakouan.app.service.reconciliation.ReconciliationOperatorStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private final FileImportRepository fileImportRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ReconciliationRunRepository runRepository;
    private final ReconciliationResultRepository resultRepository;
    private final List<ReconciliationOperatorStrategy> operatorStrategies;

    @Override
    @Transactional
    public ReconciliationRun run(ReconciliationRunRequest request) {
        OperatorType operator = request.operator() == null ? OperatorType.MOOV : request.operator();
        LocalDate from = request.businessDate() != null ? request.businessDate() : request.dateFrom();
        LocalDate to = request.businessDate() != null ? request.businessDate() : request.dateTo();
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date metier ou la plage de dates est obligatoire.");
        }

        List<FileImport> bankImports = fileImportRepository.findBySourceTypeAndOperatorScopeAndBusinessDateBetween(SourceType.BANQUE, operator, from, to);
        ReconciliationOperatorStrategy strategy = selectedStrategy(operator);
        List<FileImport> operatorImports = strategy.findImports(from, to);
        validateRunInputs(operator, from, to, bankImports, operatorImports);

        String bankImportIds = importIdsCsv(bankImports);
        String operatorImportIds = importIdsCsv(operatorImports);
        rejectAlreadyReconciled(operator, from, to, bankImportIds, operatorImportIds);

        ReconciliationRun run = ReconciliationRun.builder()
                .label(request.label())
                .operator(operator)
                .businessDateFrom(from)
                .businessDateTo(to)
                .bankImportIds(bankImportIds)
                .moovImportIds(strategy.operatorSourceType() == SourceType.MOOV ? operatorImportIds : "")
                .orangeImportIds(strategy.operatorSourceType() == SourceType.ORANGE ? operatorImportIds : "")
                .startedAt(OffsetDateTime.now())
                .status(ReconciliationRunStatus.RUNNING)
                .build();
        run = runRepository.save(run);

        List<BankTransaction> bankRows = bankTransactionRepository.findByFileImportIdIn(bankImports.stream().map(FileImport::getId).toList());
        strategy.reconcile(run, bankRows, operatorImports);

        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setFinishedAt(OffsetDateTime.now());
        ReconciliationSummaryDto summary = summary(run.getId());
        run.setSummaryJson(summary.toString());
        return runRepository.save(run);
    }


    @Override
    public Page<ReconciliationRun> runs(OperatorType operator, Pageable pageable) {
        if (operator == null) {
            return runRepository.findAll(pageable);
        }
        return runRepository.findByOperator(operator, pageable);
    }

    @Override
    public Page<ReconciliationRun> runsByDateRange(OperatorType operator, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        if (operator == null) {
            return runRepository.findByBusinessDateOverlap(dateFrom, dateTo, pageable);
        }
        return runRepository.findByOperatorAndBusinessDateOverlap(operator, dateFrom, dateTo, pageable);
    }

    @Override
    public ReconciliationRun runById(Long runId) {
        return runRepository.findById(runId).orElseThrow();
    }

    @Override
    public Page<ReconciliationResult> results(Long runId, Pageable pageable) {
        return resultRepository.findByRunId(runId, pageable);
    }

    @Override
    public Page<ReconciliationResult> resultsByType(Long runId, ReconciliationResultType type, Pageable pageable) {
        if (type == ReconciliationResultType.ABSENT_COTE_MOOV) {
            return resultRepository.findAll((root, query, cb) -> cb.and(
                    cb.equal(root.get("run").get("id"), runId),
                    cb.or(
                            cb.equal(root.get("resultType"), ReconciliationResultType.ABSENT_COTE_MOOV),
                            cb.equal(root.get("resultType"), ReconciliationResultType.ABSENT_COTE_ORANGE)
                    )
            ), pageable);
        }
        return resultRepository.findByRunIdAndResultType(runId, type, pageable);
    }

    @Override
    public ReconciliationSummaryDto summary(Long runId) {
        List<ReconciliationResult> rows = resultRepository.findByRunId(runId, Pageable.unpaged()).getContent();
        return buildSummary(rows);
    }

    @Override
    public Page<ReconciliationResult> globalResults(LocalDate dateFrom, LocalDate dateTo, ReconciliationResultType type, OperatorType operator, Pageable pageable) {
        if (operator != null) {
            Optional<Long> latestRunId = findLatestRunId(operator, dateFrom, dateTo);
            if (latestRunId.isPresent()) {
                Specification<ReconciliationResult> latestRunSpec = (root, query, cb) ->
                        cb.equal(root.get("run").get("id"), latestRunId.get());
                Specification<ReconciliationResult> typeSpec = type == null ? null : (root, query, cb) -> {
                    if (type == ReconciliationResultType.ABSENT_COTE_MOOV) {
                        return cb.or(
                                cb.equal(root.get("resultType"), ReconciliationResultType.ABSENT_COTE_MOOV),
                                cb.equal(root.get("resultType"), ReconciliationResultType.ABSENT_COTE_ORANGE)
                        );
                    }
                    return cb.equal(root.get("resultType"), type);
                };
                Specification<ReconciliationResult> finalSpec = typeSpec == null
                        ? latestRunSpec
                        : latestRunSpec.and(typeSpec);
                return resultRepository.findAll(finalSpec, pageable);
            }
        }
        return resultRepository.findAll(globalSpec(dateFrom, dateTo, type, operator), pageable);
    }

    @Override
    public ReconciliationSummaryDto globalSummary(LocalDate dateFrom, LocalDate dateTo, OperatorType operator) {
        if (operator != null) {
            Optional<Long> latestRunId = findLatestRunId(operator, dateFrom, dateTo);
            if (latestRunId.isPresent()) {
                List<ReconciliationResult> rows = resultRepository.findByRunId(latestRunId.get(), Pageable.unpaged()).getContent();
                return buildSummary(rows);
            }
        }
        List<ReconciliationResult> rows = resultRepository.findAll(globalSpec(dateFrom, dateTo, null, operator));
        return buildSummary(rows);
    }

    private Optional<Long> findLatestRunId(OperatorType operator, LocalDate dateFrom, LocalDate dateTo) {
        List<ReconciliationRun> runs;
        if (dateFrom != null && dateTo != null) {
            runs = runRepository.findByOperatorAndBusinessDateOverlap(operator, dateFrom, dateTo, Pageable.unpaged()).getContent();
        } else {
            runs = runRepository.findByOperator(operator, Pageable.unpaged()).getContent();
        }
        return runs.stream()
                .filter(r -> r.getStatus() == ReconciliationRunStatus.COMPLETED)
                .filter(r -> r.getId() != null)
                .filter(r -> r.getStartedAt() != null)
                .filter(r -> resultRepository.countByRunId(r.getId()) > 0)
                .max(Comparator.comparing(ReconciliationRun::getStartedAt))
                .map(ReconciliationRun::getId);
    }

    private ReconciliationSummaryDto buildSummary(List<ReconciliationResult> rows) {
        List<ReconciliationResult> financialRows = rows.stream()
                .filter(this::isFinanciallyRelevant)
                .toList();
        long totalResults = financialRows.size();
        long match = count(financialRows, ReconciliationResultType.MATCH_OK);
        long echecDeuxCotes = count(financialRows, ReconciliationResultType.ECHEC_DES_DEUX_COTES);
        long debit = count(financialRows, ReconciliationResultType.DEBIT_A_TORT);
        long credit = count(financialRows, ReconciliationResultType.CREDIT_SANS_DEBIT);
        long absentBank = count(financialRows, ReconciliationResultType.ABSENT_COTE_BANQUE);
        long absentMoov = count(financialRows, ReconciliationResultType.ABSENT_COTE_MOOV)
                + count(financialRows, ReconciliationResultType.ABSENT_COTE_ORANGE);
        long montantDiff = count(financialRows, ReconciliationResultType.MONTANT_DIFFERENT);
        long doublons = count(financialRows, ReconciliationResultType.DOUBLON_BANQUE) + count(financialRows, ReconciliationResultType.DOUBLON_MOOV);
        long totalEchecs = totalResults - match;

        BigDecimal totalBank = financialRows.stream().map(ReconciliationResult::getBankAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMoov = financialRows.stream().map(ReconciliationResult::getMoovAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReconciliationSummaryDto(
                financialRows.stream().filter(r -> r.getBankTransactionId() != null).count(),
                financialRows.stream().filter(r -> r.getMoovTransactionId() != null).count(),
                match, echecDeuxCotes, debit, credit, absentBank, absentMoov, montantDiff, doublons,
                toRate(match, totalResults),
                toRate(totalEchecs, totalResults),
                toRate(echecDeuxCotes, totalResults),
                toRate(debit, totalResults),
                toRate(credit, totalResults),
                toRate(absentBank, totalResults),
                toRate(absentMoov, totalResults),
                toRate(montantDiff, totalResults),
                toRate(doublons, totalResults),
                totalBank, totalMoov, totalBank.subtract(totalMoov)
        );
    }

    private boolean isFinanciallyRelevant(ReconciliationResult row) {
        return row.getResultType() != ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE
                && row.getResultType() != ReconciliationResultType.APPROVISIONNEMENT;
    }

    private Specification<ReconciliationResult> globalSpec(LocalDate dateFrom, LocalDate dateTo, ReconciliationResultType type, OperatorType operator) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("businessDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("businessDate"), dateTo));
            }
            if (type != null) {
                if (type == ReconciliationResultType.ABSENT_COTE_MOOV) {
                    predicates.add(cb.or(
                            cb.equal(root.get("resultType"), ReconciliationResultType.ABSENT_COTE_MOOV),
                            cb.equal(root.get("resultType"), ReconciliationResultType.ABSENT_COTE_ORANGE)
                    ));
                } else {
                    predicates.add(cb.equal(root.get("resultType"), type));
                }
            }
            if (operator != null) {
                predicates.add(cb.equal(root.get("run").get("operator"), operator));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private long count(List<ReconciliationResult> rows, ReconciliationResultType type) {
        return rows.stream().filter(r -> r.getResultType() == type).count();
    }

    private BigDecimal toRate(long count, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private boolean isStrategySelected(OperatorType operator, SourceType sourceType) {
        return (operator == OperatorType.MOOV && sourceType == SourceType.MOOV)
                || (operator == OperatorType.ORANGE && sourceType == SourceType.ORANGE);
    }

    private ReconciliationOperatorStrategy selectedStrategy(OperatorType operator) {
        return operatorStrategies.stream()
                .filter(strategy -> isStrategySelected(operator, strategy.operatorSourceType()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Strategie operateur introuvable pour " + operator));
    }

    private void validateRunInputs(
            OperatorType operator,
            LocalDate from,
            LocalDate to,
            List<FileImport> bankImports,
            List<FileImport> operatorImports
    ) {
        if (bankImports.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Aucun import Banque " + operator + " trouve pour la periode " + periodLabel(from, to) + ". Importez d'abord le fichier Banque."
            );
        }
        if (operatorImports.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Aucun import " + operator + " trouve pour la periode " + periodLabel(from, to) + ". Importez d'abord le fichier operateur."
            );
        }
        if (!hasValidRows(bankImports)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Les imports Banque " + operator + " de la periode " + periodLabel(from, to) + " ne contiennent aucune ligne valide."
            );
        }
        if (!hasValidRows(operatorImports)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Les imports " + operator + " de la periode " + periodLabel(from, to) + " ne contiennent aucune ligne valide."
            );
        }
    }

    private void rejectAlreadyReconciled(
            OperatorType operator,
            LocalDate from,
            LocalDate to,
            String bankImportIds,
            String operatorImportIds
    ) {
        List<ReconciliationRun> runs = runRepository.findByOperatorAndBusinessDateOverlap(operator, from, to, Pageable.unpaged()).getContent();
        boolean alreadyDone = runs.stream()
                .filter(run -> run.getStatus() == ReconciliationRunStatus.COMPLETED)
                .anyMatch(run -> Objects.equals(normalizeCsvIds(run.getBankImportIds()), bankImportIds)
                        && Objects.equals(normalizeCsvIds(operator == OperatorType.MOOV ? run.getMoovImportIds() : run.getOrangeImportIds()), operatorImportIds));
        if (alreadyDone) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ces imports ont deja ete rapproches. Importez un nouveau fichier ou supprimez le run existant avant de relancer."
            );
        }
    }

    private boolean hasValidRows(List<FileImport> imports) {
        return imports.stream()
                .map(FileImport::getValidRows)
                .filter(Objects::nonNull)
                .anyMatch(count -> count > 0);
    }

    private String importIdsCsv(List<FileImport> imports) {
        return imports.stream()
                .map(FileImport::getId)
                .filter(Objects::nonNull)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String normalizeCsvIds(String csvIds) {
        if (csvIds == null || csvIds.isBlank()) {
            return "";
        }
        return Arrays.stream(csvIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String periodLabel(LocalDate from, LocalDate to) {
        return Objects.equals(from, to) ? String.valueOf(from) : from + " -> " + to;
    }

}

