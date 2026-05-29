package com.bakouan.app.controller;

import com.bakouan.app.dto.AccountingCheckRowDto;
import com.bakouan.app.dto.AccountingKpiDto;
import com.bakouan.app.dto.AmplitudeCleanupResultDto;
import com.bakouan.app.enums.AccountingStatus;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.AmplitudeTransaction;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.repositories.AmplitudeTransactionRepository;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.FileImportRepository;
import com.bakouan.app.service.AmplitudeCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingController {
    private final BankTransactionRepository bankTransactionRepository;
    private final AmplitudeTransactionRepository amplitudeTransactionRepository;
    private final com.bakouan.app.repositories.FileImportRepository fileImportRepository;
    private final AmplitudeCleanupService amplitudeCleanupService;

    @GetMapping("/check")
    public List<AccountingCheckRowDto> check(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        var bankImportIds = fileImportRepository.findBySourceTypeAndBusinessDateBetween(SourceType.BANQUE, dateFrom, dateTo)
                .stream().map(f -> f.getId()).collect(Collectors.toSet());
        var amplitudeImportIds = fileImportRepository.findBySourceTypeAndBusinessDateBetween(SourceType.AMPLITUDE, dateFrom, dateTo)
                .stream().map(f -> f.getId()).collect(Collectors.toSet());

        List<BankTransaction> banks = bankImportIds.isEmpty() ? List.of() : bankTransactionRepository.findByFileImportIdIn(bankImportIds);
        List<AmplitudeTransaction> amplitudes = amplitudeImportIds.isEmpty() ? List.of() : amplitudeTransactionRepository.findByFileImportIdIn(amplitudeImportIds);

        Map<String, List<AmplitudeTransaction>> amplitudeByRef = amplitudes.stream()
                .filter(a -> normalizeReference(a.getOperationReference()) != null)
                .collect(Collectors.groupingBy(a -> normalizeReference(a.getOperationReference())));

        List<AccountingCheckRowDto> out = new ArrayList<>();
        for (BankTransaction bank : banks) {
            AccountingStatus status = AccountingStatus.NON_COMPTABILISE;
            AmplitudeTransaction matched = findMatchedAmplitude(bank, amplitudeByRef, amplitudes);
            if (matched != null) status = AccountingStatus.COMPTABILISE;
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
                    status
            ));
        }
        return out;
    }

    @GetMapping("/kpi")
    public AccountingKpiDto kpi(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        var bankImportIds = fileImportRepository.findBySourceTypeAndBusinessDateBetween(SourceType.BANQUE, dateFrom, dateTo)
                .stream().map(f -> f.getId()).collect(Collectors.toSet());
        var amplitudeImportIds = fileImportRepository.findBySourceTypeAndBusinessDateBetween(SourceType.AMPLITUDE, dateFrom, dateTo)
                .stream().map(f -> f.getId()).collect(Collectors.toSet());

        List<BankTransaction> banks = bankImportIds.isEmpty() ? List.of() : bankTransactionRepository.findByFileImportIdIn(bankImportIds);
        List<AmplitudeTransaction> amplitudes = amplitudeImportIds.isEmpty() ? List.of() : amplitudeTransactionRepository.findByFileImportIdIn(amplitudeImportIds);

        Map<String, List<AmplitudeTransaction>> amplitudeByRef = amplitudes.stream()
                .filter(a -> normalizeReference(a.getOperationReference()) != null)
                .collect(Collectors.groupingBy(a -> normalizeReference(a.getOperationReference())));

        long totalTransactions = banks.size();
        BigDecimal totalAmount = sumBankAmounts(banks);
        long comptabilizedTransactions = 0;
        BigDecimal comptabilizedAmount = BigDecimal.ZERO;
        long nonComptabilizedTransactions = 0;
        BigDecimal nonComptabilizedAmount = BigDecimal.ZERO;

        BigDecimal matchedAmplitudeAmount = BigDecimal.ZERO;
        BigDecimal netGapAmount = BigDecimal.ZERO;
        BigDecimal absoluteGapAmount = BigDecimal.ZERO;
        long transactionsWithGapCount = 0;
        BigDecimal totalGapAmount = BigDecimal.ZERO;

        for (BankTransaction bank : banks) {
            AmplitudeTransaction matched = findMatchedAmplitude(bank, amplitudeByRef, amplitudes);
            BigDecimal bankAmount = bank.getAmount() == null ? BigDecimal.ZERO : bank.getAmount();
            if (matched != null) {
                comptabilizedTransactions++;
                comptabilizedAmount = comptabilizedAmount.add(bankAmount);
                BigDecimal ampAmount = matched.getAmount() == null ? BigDecimal.ZERO : matched.getAmount();
                matchedAmplitudeAmount = matchedAmplitudeAmount.add(ampAmount);
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

        BigDecimal comptabilizationRateCount = rate(comptabilizedTransactions, totalTransactions);
        BigDecimal comptabilizationRateAmount = rate(comptabilizedAmount, totalAmount);
        BigDecimal nonComptabilizationRateCount = rate(nonComptabilizedTransactions, totalTransactions);
        BigDecimal amountAtRisk = nonComptabilizedAmount;

        return new AccountingKpiDto(
                totalTransactions,
                totalAmount,
                comptabilizedTransactions,
                comptabilizedAmount,
                comptabilizationRateCount,
                comptabilizationRateAmount,
                nonComptabilizedTransactions,
                nonComptabilizedAmount,
                nonComptabilizationRateCount,
                amountAtRisk,
                netGapAmount,
                absoluteGapAmount,
                transactionsWithGapCount,
                totalGapAmount
        );
    }

    @GetMapping(value = "/kpi/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportKpiCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        AccountingKpiDto k = kpi(dateFrom, dateTo);
        StringBuilder csv = new StringBuilder();
        csv.append("kpi,valeur\n");
        csv.append("total_transactions,").append(k.totalTransactions()).append('\n');
        csv.append("total_amount,").append(k.totalAmount()).append('\n');
        csv.append("comptabilized_transactions,").append(k.comptabilizedTransactions()).append('\n');
        csv.append("comptabilized_amount,").append(k.comptabilizedAmount()).append('\n');
        csv.append("comptabilization_rate_count,").append(k.comptabilizationRateCount()).append('\n');
        csv.append("comptabilization_rate_amount,").append(k.comptabilizationRateAmount()).append('\n');
        csv.append("non_comptabilized_transactions,").append(k.nonComptabilizedTransactions()).append('\n');
        csv.append("non_comptabilized_amount,").append(k.nonComptabilizedAmount()).append('\n');
        csv.append("non_comptabilization_rate_count,").append(k.nonComptabilizationRateCount()).append('\n');
        csv.append("amount_at_risk,").append(k.amountAtRisk()).append('\n');
        csv.append("net_gap_amount,").append(k.netGapAmount()).append('\n');
        csv.append("absolute_gap_amount,").append(k.absoluteGapAmount()).append('\n');
        csv.append("transactions_with_gap_count,").append(k.transactionsWithGapCount()).append('\n');
        csv.append("total_gap_amount,").append(k.totalGapAmount()).append('\n');
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping(value = "/kpi/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportKpiPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        AccountingKpiDto k = kpi(dateFrom, dateTo);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 800f;
                cs.beginText(); cs.setFont(bold, 14); cs.newLineAtOffset(40, y); cs.showText("KPI Comptabilisation Carthago / AMPLITUDE"); cs.endText();
                y -= 20;
                cs.beginText(); cs.setFont(font, 10); cs.newLineAtOffset(40, y); cs.showText("Periode: " + dateFrom + " -> " + dateTo); cs.endText();
                y -= 25;
                String[] lines = new String[]{
                        "1) Volume total Carthago",
                        "   - Nb transactions: " + k.totalTransactions(),
                        "   - Montant total: " + k.totalAmount(),
                        "2) Operations comptabilisees",
                        "   - Nb: " + k.comptabilizedTransactions() + " | Montant: " + k.comptabilizedAmount(),
                        "   - Taux nb: " + k.comptabilizationRateCount() + "% | Taux montant: " + k.comptabilizationRateAmount() + "%",
                        "3) Operations non comptabilisees",
                        "   - Nb: " + k.nonComptabilizedTransactions() + " | Montant: " + k.nonComptabilizedAmount(),
                        "   - Taux non compta: " + k.nonComptabilizationRateCount() + "% | Montant a risque: " + k.amountAtRisk(),
                        "4) Ecart financier",
                        "   - Ecart net: " + k.netGapAmount() + " | Ecart absolu total: " + k.absoluteGapAmount(),
                        "   - Nb tx avec ecart: " + k.transactionsWithGapCount() + " | Montant total des ecarts: " + k.totalGapAmount()
                };
                for (String line : lines) {
                    cs.beginText(); cs.setFont(font, 10); cs.newLineAtOffset(40, y); cs.showText(line); cs.endText();
                    y -= 15;
                }
            }
            document.save(out);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de generer le PDF KPI", e);
        }
    }

    private boolean fallbackComposite(BankTransaction bank, List<AmplitudeTransaction> amplitudes) {
        if (bank.getTransactionDate() == null || bank.getAmount() == null) return false;
        LocalDate bankDate = bank.getTransactionDate().toLocalDate();
        String bankAccount = normalize(bank.getAccountNumber());
        BigDecimal bankAmount = bank.getAmount();

        for (AmplitudeTransaction a : amplitudes) {
            if (a.getOperationDate() == null || a.getAmount() == null) continue;
            long days = Math.abs(ChronoUnit.DAYS.between(bankDate, a.getOperationDate().toLocalDate()));
            if (days > 1) continue;
            if (bankAmount.compareTo(a.getAmount()) != 0) continue;
            String ampAccount = normalize(a.getAccountNumber());
            if (bankAccount != null && ampAccount != null && !bankAccount.equals(ampAccount)) continue;
            return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return null;
        String v = value.replaceAll("\\D", "");
        return v.isBlank() ? null : v;
    }

    private BigDecimal sumBankAmounts(List<BankTransaction> banks) {
        return banks.stream()
                .map(BankTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (numerator == null) numerator = BigDecimal.ZERO;
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
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
        if (fallbackComposite(bank, amplitudes)) {
            return amplitudes.stream()
                    .filter(a -> a.getOperationDate() != null && a.getAmount() != null)
                    .filter(a -> bank.getTransactionDate() != null && bank.getAmount() != null)
                    .filter(a -> Math.abs(ChronoUnit.DAYS.between(bank.getTransactionDate().toLocalDate(), a.getOperationDate().toLocalDate())) <= 1)
                    .filter(a -> bank.getAmount().compareTo(a.getAmount()) == 0)
                    .filter(a -> {
                        String bankAccount = normalize(bank.getAccountNumber());
                        String ampAccount = normalize(a.getAccountNumber());
                        return bankAccount == null || ampAccount == null || bankAccount.equals(ampAccount);
                    })
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String normalizeReference(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) return null;
        return digits.length() < 10 ? null : digits;
    }

    @DeleteMapping("/amplitude/cleanup/daily")
    public AmplitudeCleanupResultDto cleanupAmplitudeDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate
    ) {
        return amplitudeCleanupService.cleanupDaily(businessDate);
    }

    @DeleteMapping("/amplitude/cleanup/all")
    public AmplitudeCleanupResultDto cleanupAmplitudeAll() {
        return amplitudeCleanupService.cleanupAll();
    }

    @DeleteMapping("/amplitude/cleanup/range")
    public AmplitudeCleanupResultDto cleanupAmplitudeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return amplitudeCleanupService.cleanupRange(dateFrom, dateTo);
    }

    @DeleteMapping("/amplitude/cleanup/weekly")
    public AmplitudeCleanupResultDto cleanupAmplitudeWeekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate
    ) {
        return amplitudeCleanupService.cleanupWeekly(referenceDate);
    }
}
