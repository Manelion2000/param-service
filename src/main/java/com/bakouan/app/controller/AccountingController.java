package com.bakouan.app.controller;

import com.bakouan.app.dto.AccountingCheckRowDto;
import com.bakouan.app.dto.AccountingKpiDto;
import com.bakouan.app.dto.AmplitudeCleanupResultDto;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.service.AccountingService;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingController {
    private final AccountingService accountingService;
    private final AmplitudeCleanupService amplitudeCleanupService;

    @GetMapping("/check")
    public List<AccountingCheckRowDto> check(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return accountingService.check(dateFrom, dateTo);
    }

    @GetMapping("/amplitude/bankMoov/check")
    public List<AccountingCheckRowDto> checkMoov(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return accountingService.check(dateFrom, dateTo, OperatorType.MOOV);
    }

    @GetMapping("/amplitude/bankOrange/check")
    public List<AccountingCheckRowDto> checkOrange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return accountingService.check(dateFrom, dateTo, OperatorType.ORANGE);
    }

    @GetMapping("/kpi")
    public AccountingKpiDto kpi(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return accountingService.kpi(dateFrom, dateTo);
    }

    @GetMapping("/amplitude/bankMoov/kpi")
    public AccountingKpiDto kpiMoov(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return accountingService.kpi(dateFrom, dateTo, OperatorType.MOOV);
    }

    @GetMapping("/amplitude/bankOrange/kpi")
    public AccountingKpiDto kpiOrange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return accountingService.kpi(dateFrom, dateTo, OperatorType.ORANGE);
    }

    @GetMapping("/carthago/latest-date")
    public LocalDate latestCarthagoDate(
            @RequestParam(value = "operator", required = false) OperatorType operator
    ) {
        return accountingService.latestCarthagoDate(operator);
    }

    @GetMapping(value = "/kpi/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportKpiCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return exportKpiCsv(dateFrom, dateTo, null);
    }

    @GetMapping(value = "/amplitude/bankMoov/kpi/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportKpiCsvMoov(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return exportKpiCsv(dateFrom, dateTo, OperatorType.MOOV);
    }

    @GetMapping(value = "/amplitude/bankOrange/kpi/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportKpiCsvOrange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return exportKpiCsv(dateFrom, dateTo, OperatorType.ORANGE);
    }

    private ResponseEntity<byte[]> exportKpiCsv(LocalDate dateFrom, LocalDate dateTo, OperatorType operator) {
        AccountingKpiDto k = operator == null ? kpi(dateFrom, dateTo) : accountingService.kpi(dateFrom, dateTo, operator);
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
        return exportKpiPdf(dateFrom, dateTo, null);
    }

    @GetMapping(value = "/amplitude/bankMoov/kpi/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportKpiPdfMoov(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return exportKpiPdf(dateFrom, dateTo, OperatorType.MOOV);
    }

    @GetMapping(value = "/amplitude/bankOrange/kpi/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportKpiPdfOrange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return exportKpiPdf(dateFrom, dateTo, OperatorType.ORANGE);
    }

    private ResponseEntity<byte[]> exportKpiPdf(LocalDate dateFrom, LocalDate dateTo, OperatorType operator) {
        AccountingKpiDto k = operator == null ? kpi(dateFrom, dateTo) : accountingService.kpi(dateFrom, dateTo, operator);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 800f;
                String title = operator == null
                        ? "KPI Comptabilisation Carthago / AMPLITUDE"
                        : "KPI Comptabilisation Carthago / AMPLITUDE - " + operator.name();
                cs.beginText(); cs.setFont(bold, 14); cs.newLineAtOffset(40, y); cs.showText(title); cs.endText();
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
