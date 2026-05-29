package com.bakouan.app.controller;

import com.bakouan.app.dto.ReconciliationRunRequest;
import com.bakouan.app.dto.ReconciliationSummaryDto;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.service.ReconciliationService;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/reconciliations")
@RequiredArgsConstructor
public class ReconciliationController {

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT)
    };

    private final ReconciliationService reconciliationService;
    private final ReconciliationResultRepository reconciliationResultRepository;

    @PostMapping("/run")
    public ReconciliationRun run(@Valid @RequestBody ReconciliationRunRequest request) {
        return reconciliationService.run(request);
    }

    @GetMapping("/runs")
    public Page<ReconciliationRun> runs(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String singleDay,
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) OperatorType operator,
            Pageable pageable) {
        LocalDate parsedFrom = parseDateParam(dateFrom, "dateFrom");
        LocalDate parsedTo = parseDateParam(dateTo, "dateTo");
        LocalDate parsedSingleDay = parseDateParam(singleDay, "singleDay");

        if (parsedSingleDay != null) {
            return reconciliationService.runsByDateRange(operator, parsedSingleDay, parsedSingleDay, pageable);
        }
        if (preset != null && !preset.isBlank()) {
            LocalDate end = parsedTo != null ? parsedTo : LocalDate.now();
            LocalDate start = switch (preset) {
                case "LAST_7_DAYS" -> end.minusDays(6);
                case "LAST_30_DAYS" -> end.minusDays(29);
                case "LAST_3_MONTHS" -> end.minusMonths(3).plusDays(1);
                default -> null;
            };
            if (start != null) {
                return reconciliationService.runsByDateRange(operator, start, end, pageable);
            }
        }
        if (parsedFrom != null && parsedTo != null) {
            return reconciliationService.runsByDateRange(operator, parsedFrom, parsedTo, pageable);
        }
        return reconciliationService.runs(operator, pageable);
    }

    @GetMapping("/runs/{id}")
    public ReconciliationRun run(@PathVariable Long id) {
        return reconciliationService.runById(id);
    }

    @GetMapping("/runs/{id}/results")
    public Page<ReconciliationResult> results(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.results(id, pageable);
    }

    @GetMapping("/runs/{id}/summary")
    public ReconciliationSummaryDto summary(@PathVariable Long id) {
        return reconciliationService.summary(id);
    }

    @GetMapping("/summary")
    public ReconciliationSummaryDto globalSummary(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String singleDay,
            @RequestParam(required = false) OperatorType operator) {
        LocalDate parsedFrom = parseDateParam(dateFrom, "dateFrom");
        LocalDate parsedTo = parseDateParam(dateTo, "dateTo");
        LocalDate parsedSingleDay = parseDateParam(singleDay, "singleDay");

        LocalDate from = parsedSingleDay != null ? parsedSingleDay : parsedFrom;
        LocalDate to = parsedSingleDay != null ? parsedSingleDay : parsedTo;
        return reconciliationService.globalSummary(from, to, operator);
    }

    @GetMapping("/results")
    public Page<ReconciliationResult> globalResults(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String singleDay,
            @RequestParam(required = false) ReconciliationResultType type,
            @RequestParam(required = false) OperatorType operator,
            @RequestParam(required = false) String operationReference,
            Pageable pageable) {
        LocalDate parsedFrom = parseDateParam(dateFrom, "dateFrom");
        LocalDate parsedTo = parseDateParam(dateTo, "dateTo");
        LocalDate parsedSingleDay = parseDateParam(singleDay, "singleDay");

        LocalDate from = parsedSingleDay != null ? parsedSingleDay : parsedFrom;
        LocalDate to = parsedSingleDay != null ? parsedSingleDay : parsedTo;
        if (operationReference == null || operationReference.isBlank()) {
            return reconciliationService.globalResults(from, to, type, operator, pageable);
        }
        String opRef = operationReference.trim();
        return reconciliationResultRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("businessDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("businessDate"), to));
            if (type != null) predicates.add(cb.equal(root.get("resultType"), type));
            if (operator != null) predicates.add(cb.equal(root.get("run").get("operator"), operator));
            var bankSub = query.subquery(Long.class);
            var bankRoot = bankSub.from(com.bakouan.app.model.BankTransaction.class);
            bankSub.select(bankRoot.get("id"))
                    .where(
                            cb.equal(bankRoot.get("id"), root.get("bankTransactionId")),
                            cb.equal(bankRoot.get("operationReference"), opRef)
                    );
            predicates.add(cb.exists(bankSub));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
    }

    @GetMapping("/runs/{id}/matches")
    public Page<ReconciliationResult> matches(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.MATCH_OK, pageable);
    }

    @GetMapping("/runs/{id}/debits-a-tort")
    public Page<ReconciliationResult> debitATort(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.DEBIT_A_TORT, pageable);
    }

    @GetMapping("/runs/{id}/credits-sans-debit")
    public Page<ReconciliationResult> creditSansDebit(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.CREDIT_SANS_DEBIT, pageable);
    }

    @GetMapping("/runs/{id}/echecs")
    public Page<ReconciliationResult> echecs(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.ECHEC_DES_DEUX_COTES, pageable);
    }

    @GetMapping("/runs/{id}/absents-banque")
    public Page<ReconciliationResult> absentBanque(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.ABSENT_COTE_BANQUE, pageable);
    }

    @GetMapping("/runs/{id}/absents-moov")
    public Page<ReconciliationResult> absentMoov(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.ABSENT_COTE_MOOV, pageable);
    }

    @GetMapping("/runs/{id}/absents-orange")
    public Page<ReconciliationResult> absentOrange(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.ABSENT_COTE_ORANGE, pageable);
    }

    @GetMapping("/runs/{id}/doublons")
    public Page<ReconciliationResult> doublons(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.DOUBLON_BANQUE, pageable);
    }

    @GetMapping("/runs/{id}/montants-differents")
    public Page<ReconciliationResult> montantsDifferents(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.MONTANT_DIFFERENT, pageable);
    }

    @GetMapping("/history/by-date")
    public List<ReconciliationRun> byDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return reconciliationService.runsByDateRange(null, businessDate, businessDate, Pageable.unpaged()).getContent();
    }

    @GetMapping("/history/range")
    public List<ReconciliationRun> byRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return reconciliationService.runsByDateRange(null, dateFrom, dateTo, Pageable.unpaged()).getContent();
    }

    @GetMapping("/history/latest")
    public ReconciliationRun latest() {
        return reconciliationService.runs(null, Pageable.unpaged()).getContent().stream()
                .filter(r -> r.getStartedAt() != null)
                .max(Comparator.comparing(ReconciliationRun::getStartedAt))
                .orElse(null);
    }

    @GetMapping("/history/reset")
    public Page<ReconciliationRun> resetHistory(Pageable pageable) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        return reconciliationService.runsByDateRange(null, start, end, pageable);
    }

    @GetMapping(value = "/runs/{id}/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long id) {
        String csv = "transactionKey,resultType\n" + reconciliationService.results(id, Pageable.unpaged()).getContent().stream()
                .map(r -> r.getTransactionKey() + "," + r.getResultType())
                .reduce("", (a, b) -> a + b + "\n");
        return ResponseEntity.ok().contentType(new MediaType("text", "csv"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/runs/{id}/export/xlsx")
    public ResponseEntity<String> exportXlsx(@PathVariable Long id) {
        return ResponseEntity.ok("Export XLSX endpoint pret a etre connecte au generateur Apache POI.");
    }

    private LocalDate parseDateParam(String value, String paramName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // try next supported format
            }
        }
        throw new ResponseStatusException(
                BAD_REQUEST,
                "Format de date invalide pour '" + paramName + "': " + value
                        + ". Formats acceptes: yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy"
        );
    }
}
