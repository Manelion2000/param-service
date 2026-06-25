package com.bakouan.app.controller;

import com.bakouan.app.dto.ReconciliationRunRequest;
import com.bakouan.app.dto.ReconciliationSummaryDto;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.service.ReconciliationService;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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

import java.io.ByteArrayOutputStream;
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
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String transactionKey,
            Pageable pageable) {
        LocalDate parsedFrom = parseDateParam(dateFrom, "dateFrom");
        LocalDate parsedTo = parseDateParam(dateTo, "dateTo");
        LocalDate parsedSingleDay = parseDateParam(singleDay, "singleDay");

        LocalDate from = parsedSingleDay != null ? parsedSingleDay : parsedFrom;
        LocalDate to = parsedSingleDay != null ? parsedSingleDay : parsedTo;
        boolean hasAdvancedFilter = hasText(operationReference) || hasText(phoneNumber)
                || hasText(accountNumber) || hasText(transactionKey);
        if (!hasAdvancedFilter) {
            return reconciliationService.globalResults(from, to, type, operator, pageable);
        }
        String opRef = trimToNull(operationReference);
        String phone = trimToNull(phoneNumber);
        String account = trimToNull(accountNumber);
        String key = trimToNull(transactionKey);
        return reconciliationResultRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("businessDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("businessDate"), to));
            if (type != null) predicates.add(cb.equal(root.get("resultType"), type));
            if (operator != null) predicates.add(cb.equal(root.get("run").get("operator"), operator));
            if (key != null) {
                predicates.add(cb.like(cb.lower(root.get("transactionKey")), "%" + key.toLowerCase(Locale.ROOT) + "%"));
            }
            if (opRef != null || phone != null || account != null) {
                var bankSub = query.subquery(Long.class);
                var bankRoot = bankSub.from(BankTransaction.class);
                var bankPredicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                bankPredicates.add(cb.equal(bankRoot.get("id"), root.get("bankTransactionId")));
                if (opRef != null) bankPredicates.add(cb.equal(bankRoot.get("operationReference"), opRef));
                if (phone != null) bankPredicates.add(cb.like(cb.lower(bankRoot.get("phoneNumber")), "%" + phone.toLowerCase(Locale.ROOT) + "%"));
                if (account != null) bankPredicates.add(cb.like(cb.lower(bankRoot.get("accountNumber")), "%" + account.toLowerCase(Locale.ROOT) + "%"));
                bankSub.select(bankRoot.get("id")).where(bankPredicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                predicates.add(cb.exists(bankSub));
            }
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

    @GetMapping("/runs/{id}/operateur-non-abouti-sans-banque")
    public Page<ReconciliationResult> operatorNotCompletedWithoutBank(@PathVariable Long id, Pageable pageable) {
        return reconciliationService.resultsByType(id, ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE, pageable);
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
    public ResponseEntity<byte[]> exportXlsx(@PathVariable Long id) {
        List<ReconciliationResult> rows = reconciliationService.results(id, Pageable.unpaged()).getContent();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("reconciliation");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "transactionKey", "resultType", "businessDate", "bankTransactionId", "operatorTransactionId",
                    "bankStatus", "operatorStatus", "bankAmount", "operatorAmount", "amountDifference", "reason"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIndex = 1;
            for (ReconciliationResult result : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(nullToEmpty(result.getTransactionKey()));
                row.createCell(1).setCellValue(result.getResultType() == null ? "" : result.getResultType().name());
                row.createCell(2).setCellValue(result.getBusinessDate() == null ? "" : result.getBusinessDate().toString());
                row.createCell(3).setCellValue(result.getBankTransactionId() == null ? "" : result.getBankTransactionId().toString());
                row.createCell(4).setCellValue(result.getMoovTransactionId() == null ? "" : result.getMoovTransactionId().toString());
                row.createCell(5).setCellValue(nullToEmpty(result.getBankStatusRaw()));
                row.createCell(6).setCellValue(nullToEmpty(result.getMoovStatusRaw()));
                row.createCell(7).setCellValue(result.getBankAmount() == null ? "" : result.getBankAmount().toPlainString());
                row.createCell(8).setCellValue(result.getMoovAmount() == null ? "" : result.getMoovAmount().toPlainString());
                row.createCell(9).setCellValue(result.getAmountDifference() == null ? "" : result.getAmountDifference().toPlainString());
                row.createCell(10).setCellValue(nullToEmpty(result.getReason()));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reconciliation-run-" + id + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de generer l'export XLSX", e);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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
