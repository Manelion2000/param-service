package com.bakouan.app.service.impl;

import com.bakouan.app.dto.dashboard.*;
import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.NormalizedMoovStatus;
import com.bakouan.app.enums.NormalizedOrangeStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.OrangeTransaction;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.MoovTransactionRepository;
import com.bakouan.app.repositories.OrangeTransactionRepository;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import com.bakouan.app.repositories.ReconciliationRunRepository;
import com.bakouan.app.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private static final int MAX_DETAILS_JSON = 1000;

    private final ReconciliationRunRepository runRepository;
    private final ReconciliationResultRepository resultRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final MoovTransactionRepository moovTransactionRepository;
    private final OrangeTransactionRepository orangeTransactionRepository;

    @Override
    public ReportingSummaryDto buildSummary(ReportingPeriodType periodType, LocalDate referenceDate, OperatorType channel) {
        ReportWindow window = resolveWindow(periodType, referenceDate);
        ReportData data = loadData(window, channel);
        return toSummary(periodType, referenceDate, window, channel, data, MAX_DETAILS_JSON);
    }

    @Override
    public byte[] exportExcel(ReportingPeriodType periodType, LocalDate referenceDate, OperatorType channel) {
        ReportWindow window = resolveWindow(periodType, referenceDate);
        ReportData data = loadData(window, channel);
        ReportingSummaryDto summary = toSummary(periodType, referenceDate, window, channel, data, Integer.MAX_VALUE);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            buildSummarySheet(workbook.createSheet("Synthese"), summary);
            buildDistributionSheet(workbook.createSheet("Distribution"), summary.distribution());
            buildDailySheet(workbook.createSheet("Journalier"), summary.dailyBreakdown());
            buildDetailsSheet(workbook.createSheet("Transactions"), summary.transactionDetails());
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de generer le rapport Excel", e);
        }
    }

    @Override
    public byte[] exportPdf(ReportingPeriodType periodType, LocalDate referenceDate, OperatorType channel) {
        ReportWindow window = resolveWindow(periodType, referenceDate);
        ReportData data = loadData(window, channel);
        ReportingSummaryDto summary = toSummary(periodType, referenceDate, window, channel, data, 80);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float y = 800f;
            y = writeLine(stream, 40, y, fontBold, 14, "Rapport Reconciliation");
            y = writeLine(stream, 40, y, fontRegular, 10,
                    "Canal: " + channel + " | Periode: " + summary.dateFrom() + " -> " + summary.dateTo());
            y = writeLine(stream, 40, y - 4, fontBold, 11, "KPI Principaux");
            ReportingKpiDto k = summary.kpis();
            y = writeLine(stream, 40, y, fontRegular, 10,
                    "Transactions: " + k.totalTransactions() + " | Match: " + k.matchingCount() + " | Anomalies: " + k.anomalyCount());
            y = writeLine(stream, 40, y, fontRegular, 10,
                    "Success rate: " + k.successRate() + "% | Anomaly rate: " + k.anomalyRate() + "%");
            y = writeLine(stream, 40, y, fontRegular, 10,
                    "Montant Banque: " + k.montantTotalBanque() + " | Montant Operateur: " + k.montantTotalOperateur());
            y = writeLine(stream, 40, y, fontRegular, 10,
                    "Ecart global: " + k.ecartGlobal() + " | Montant anomalies: " + k.montantAnomalies());
            y = writeLine(stream, 40, y, fontRegular, 10,
                    "Succes operateur: " + k.operateurSuccessCount() + " | Montant: " + k.operateurSuccessAmount()
                            + " | Succes Carthago: " + k.bankSuccessCount() + " | Montant: " + k.bankSuccessAmount());
            y = writeLine(stream, 40, y, fontRegular, 10,
                    "Succes operateur sans Carthago: " + k.operateurSuccessSansCarthagoCount()
                            + " | Montant: " + k.operateurSuccessSansCarthagoAmount());
            y = writeLine(stream, 40, y, fontRegular, 10,
                    "Hors perimetre operateur: " + k.operateurHorsPerimetreCount()
                            + " | Montant: " + k.operateurHorsPerimetreAmount());
            y = writeLine(stream, 40, y - 4, fontBold, 11, "Synthese Executive");
            for (String insight : buildExecutiveInsights(summary)) {
                y = writeLine(stream, 40, y, fontRegular, 10, "- " + insight);
            }

            y = writeLine(stream, 40, y - 4, fontBold, 11, "Top Details Transactions");

            int maxLines = 24;
            int index = 1;
            for (ReportingTransactionDetailDto d : summary.transactionDetails()) {
                if (index > maxLines) {
                    break;
                }
                String line = index + ". " + d.businessDate() + " | " + safe(d.transactionKey()) + " | "
                        + d.resultType() + " | " + d.amountDifference();
                y = writeLine(stream, 40, y, fontRegular, 9, line);
                index++;
            }

            stream.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de generer le rapport PDF", e);
        }
    }

    private ReportingSummaryDto toSummary(ReportingPeriodType periodType,
                                          LocalDate referenceDate,
                                          ReportWindow window,
                                          OperatorType channel,
                                          ReportData data,
                                          int maxDetails) {
        List<ReportingRow> rows = data.rows();
        List<ReportingRow> financialRows = rows.stream().filter(this::isFinanciallyRelevant).toList();
        Map<DashboardResultTypeView, Long> distribution = buildDistribution(financialRows, channel);
        List<ReportingDailyBreakdownDto> daily = buildDailyBreakdown(financialRows, window);
        ReportingKpiDto kpis = buildKpis(rows, channel, daily);
        List<ReportingTransactionDetailDto> details = financialRows.stream()
                .sorted(Comparator.comparing((ReportingRow r) -> detailPriority(r, channel))
                        .thenComparing(ReportingRow::transactionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(r -> r.result().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(maxDetails)
                .map(r -> toDetail(r, channel))
                .toList();

        return new ReportingSummaryDto(
                periodType,
                referenceDate,
                window.from(),
                window.to(),
                channel,
                OffsetDateTime.now(),
                kpis,
                Arrays.stream(DashboardResultTypeView.values())
                        .map(v -> new ResultDistributionDto(v, distribution.getOrDefault(v, 0L)))
                        .filter(d -> d.count() > 0)
                        .toList(),
                daily,
                details
        );
    }

    private ReportData loadData(ReportWindow window, OperatorType channel) {
        Optional<ReconciliationRun> latestRun = runRepository
                .findByOperatorAndBusinessDateOverlap(channel, window.from(), window.to(), Pageable.unpaged())
                .getContent()
                .stream()
                .filter(run -> run.getStatus() == com.bakouan.app.enums.ReconciliationRunStatus.COMPLETED)
                .filter(run -> run.getStartedAt() != null)
                .max(Comparator.comparing(ReconciliationRun::getStartedAt));
        if (latestRun.isEmpty() || latestRun.get().getId() == null) {
            return new ReportData(List.of());
        }
        List<ReconciliationResult> results = resultRepository.findByRunIdIn(List.of(latestRun.get().getId()));
        return new ReportData(enrichByTransactionDate(results, channel, window));
    }

    private ReportingKpiDto buildKpis(List<ReportingRow> rows, OperatorType channel, List<ReportingDailyBreakdownDto> daily) {
        List<ReportingRow> financialRows = rows.stream().filter(this::isFinanciallyRelevant).toList();
        List<ReportingRow> operatorOutOfScopeRows = rows.stream().filter(this::isOperatorOutOfScope).toList();
        long total = financialRows.size();
        long match = count(financialRows, ReconciliationResultType.MATCH_OK);
        long anomalies = total - match;
        long debitATort = count(financialRows, ReconciliationResultType.DEBIT_A_TORT);
        long creditSansDebit = count(financialRows, ReconciliationResultType.CREDIT_SANS_DEBIT);
        long absentBanque = count(financialRows, ReconciliationResultType.ABSENT_COTE_BANQUE);
        long absentOperateur = count(financialRows, channel == OperatorType.MOOV ? ReconciliationResultType.ABSENT_COTE_MOOV : ReconciliationResultType.ABSENT_COTE_ORANGE);
        long montantDifferent = count(financialRows, ReconciliationResultType.MONTANT_DIFFERENT);
        long doublons = count(financialRows, ReconciliationResultType.DOUBLON_BANQUE) + count(financialRows, ReconciliationResultType.DOUBLON_MOOV);
        long statutInconnu = count(financialRows, ReconciliationResultType.STATUT_INCONNU);

        BigDecimal bankTotal = sum(financialRows, r -> r.result().getBankAmount());
        BigDecimal operatorTotal = sum(financialRows, r -> r.result().getMoovAmount());
        BigDecimal anomaliesAmount = financialRows.stream()
                .filter(r -> r.result().getResultType() != ReconciliationResultType.MATCH_OK)
                .map(r -> anomalyAmount(r.result()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ReportingRow> operatorSuccessRows = rows.stream().filter(ReportingRow::operatorSuccess).toList();
        List<ReportingRow> bankSuccessRows = rows.stream().filter(ReportingRow::bankSuccess).toList();
        List<ReportingRow> operatorSuccessWithoutCarthagoRows = operatorSuccessRows.stream()
                .filter(r -> r.result().getBankTransactionId() == null)
                .toList();
        BigDecimal operatorSuccessAmount = sum(operatorSuccessRows, ReportingRow::operatorAmount);
        BigDecimal bankSuccessAmount = sum(bankSuccessRows, ReportingRow::bankAmount);
        BigDecimal operatorSuccessWithoutCarthagoAmount = sum(operatorSuccessWithoutCarthagoRows, ReportingRow::operatorAmount);
        BigDecimal operatorOutOfScopeAmount = sum(operatorOutOfScopeRows, r -> r.result().getMoovAmount());
        BigDecimal avgDaily = daily.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(total).divide(BigDecimal.valueOf(daily.size()), 2, RoundingMode.HALF_UP);
        LocalVolumePeakDto peak = daily.stream()
                .max(Comparator.comparingLong(ReportingDailyBreakdownDto::totalTransactions))
                .map(d -> new LocalVolumePeakDto(d.businessDate(), d.totalTransactions()))
                .orElse(new LocalVolumePeakDto(null, 0));

        return new ReportingKpiDto(
                total,
                match,
                anomalies,
                rate(match, total),
                rate(anomalies, total),
                debitATort,
                creditSansDebit,
                absentBanque,
                absentOperateur,
                montantDifferent,
                doublons,
                statutInconnu,
                bankTotal,
                operatorTotal,
                anomaliesAmount,
                bankTotal.subtract(operatorTotal),
                operatorSuccessRows.size(),
                operatorSuccessAmount,
                bankSuccessRows.size(),
                bankSuccessAmount,
                operatorSuccessWithoutCarthagoRows.size(),
                operatorSuccessWithoutCarthagoAmount,
                operatorOutOfScopeRows.size(),
                operatorOutOfScopeAmount,
                avgDaily,
                peak
        );
    }

    private List<ReportingDailyBreakdownDto> buildDailyBreakdown(List<ReportingRow> rows, ReportWindow window) {
        Map<LocalDate, List<ReportingRow>> byDay = rows.stream()
                .filter(this::isFinanciallyRelevant)
                .filter(r -> r.transactionDate() != null)
                .collect(Collectors.groupingBy(ReportingRow::transactionDate));

        List<ReportingDailyBreakdownDto> points = new ArrayList<>();
        LocalDate cursor = window.from();
        while (!cursor.isAfter(window.to())) {
            List<ReportingRow> dayRows = byDay.getOrDefault(cursor, List.of());
            long total = dayRows.size();
            long match = count(dayRows, ReconciliationResultType.MATCH_OK);
            long anomaly = total - match;
            BigDecimal bank = sum(dayRows, r -> r.result().getBankAmount());
            BigDecimal operator = sum(dayRows, r -> r.result().getMoovAmount());
            points.add(new ReportingDailyBreakdownDto(
                    cursor,
                    total,
                    match,
                    anomaly,
                    rate(match, total),
                    bank,
                    operator,
                    bank.subtract(operator)
            ));
            cursor = cursor.plusDays(1);
        }
        return points;
    }

    private Map<DashboardResultTypeView, Long> buildDistribution(List<ReportingRow> rows, OperatorType channel) {
        Map<DashboardResultTypeView, Long> distribution = new EnumMap<>(DashboardResultTypeView.class);
        for (ReportingRow row : rows) {
            DashboardResultTypeView view = toViewType(row, channel);
            distribution.put(view, distribution.getOrDefault(view, 0L) + 1L);
        }
        return distribution;
    }

    private DashboardResultTypeView toViewType(ReportingRow row, OperatorType channel) {
        if (row.operatorSuccess() && row.result().getBankTransactionId() == null) {
            return DashboardResultTypeView.OPERATEUR_ABOUTI_SANS_CARTHAGO;
        }
        return toViewType(row.result().getResultType(), channel);
    }

    private int detailPriority(ReportingRow row, OperatorType channel) {
        DashboardResultTypeView viewType = toViewType(row, channel);
        if (viewType == DashboardResultTypeView.OPERATEUR_ABOUTI_SANS_CARTHAGO) {
            return 0;
        }
        if (viewType != DashboardResultTypeView.MATCH_OK) {
            return 1;
        }
        return 2;
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

    private ReportingTransactionDetailDto toDetail(ReportingRow row, OperatorType channel) {
        ReconciliationResult r = row.result();
        return new ReportingTransactionDetailDto(
                row.transactionDate(),
                r.getTransactionKey(),
                toViewType(row, channel),
                directionLabel(row, channel),
                r.getBankStatusRaw(),
                r.getMoovStatusRaw(),
                r.getBankAmount(),
                r.getMoovAmount(),
                r.getAmountDifference(),
                r.getReason()
        );
    }

    private String directionLabel(ReportingRow row, OperatorType channel) {
        if ("BANK_TO_WALLET".equals(row.operationType())) {
            return "BANQUE -> " + channel;
        }
        if ("WALLET_TO_BANK".equals(row.operationType())) {
            return channel + " -> BANQUE";
        }
        ReconciliationResult r = row.result();
        if (r.getBankTransactionId() != null && r.getMoovTransactionId() == null) {
            return "BANQUE -> " + channel;
        }
        if (r.getBankTransactionId() == null && r.getMoovTransactionId() != null) {
            return channel + " -> BANQUE";
        }
        return "BANQUE <-> " + channel;
    }

    private void buildSummarySheet(Sheet sheet, ReportingSummaryDto summary) {
        int rowIdx = 0;
        rowIdx = writeKv(sheet, rowIdx, "Canal", String.valueOf(summary.channel()));
        rowIdx = writeKv(sheet, rowIdx, "Type periode", String.valueOf(summary.periodType()));
        rowIdx = writeKv(sheet, rowIdx, "Date debut", String.valueOf(summary.dateFrom()));
        rowIdx = writeKv(sheet, rowIdx, "Date fin", String.valueOf(summary.dateTo()));
        ReportingKpiDto k = summary.kpis();
        rowIdx++;
        rowIdx = writeKv(sheet, rowIdx, "Total transactions", k.totalTransactions());
        rowIdx = writeKv(sheet, rowIdx, "Matching", k.matchingCount());
        rowIdx = writeKv(sheet, rowIdx, "Anomalies", k.anomalyCount());
        rowIdx = writeKv(sheet, rowIdx, "Success rate (%)", k.successRate());
        rowIdx = writeKv(sheet, rowIdx, "Anomaly rate (%)", k.anomalyRate());
        rowIdx = writeKv(sheet, rowIdx, "Montant banque", k.montantTotalBanque());
        rowIdx = writeKv(sheet, rowIdx, "Montant operateur", k.montantTotalOperateur());
        rowIdx = writeKv(sheet, rowIdx, "Montant anomalies", k.montantAnomalies());
        rowIdx = writeKv(sheet, rowIdx, "Ecart global", k.ecartGlobal());
        rowIdx = writeKv(sheet, rowIdx, "Succes operateur (count)", k.operateurSuccessCount());
        rowIdx = writeKv(sheet, rowIdx, "Succes operateur (montant)", k.operateurSuccessAmount());
        rowIdx = writeKv(sheet, rowIdx, "Succes Carthago (count)", k.bankSuccessCount());
        rowIdx = writeKv(sheet, rowIdx, "Succes Carthago (montant)", k.bankSuccessAmount());
        rowIdx = writeKv(sheet, rowIdx, "Succes operateur sans Carthago (count)", k.operateurSuccessSansCarthagoCount());
        rowIdx = writeKv(sheet, rowIdx, "Succes operateur sans Carthago (montant)", k.operateurSuccessSansCarthagoAmount());
        rowIdx = writeKv(sheet, rowIdx, "Operateur hors perimetre (count)", k.operateurHorsPerimetreCount());
        rowIdx = writeKv(sheet, rowIdx, "Operateur hors perimetre (montant)", k.operateurHorsPerimetreAmount());
        rowIdx = writeKv(sheet, rowIdx, "Moyenne journaliere", k.moyenneJournaliereTransactions());
        rowIdx = writeKv(sheet, rowIdx, "Pic volume (date)", k.picVolumeJournalier().businessDate());
        writeKv(sheet, rowIdx, "Pic volume (count)", k.picVolumeJournalier().totalTransactions());
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void buildDistributionSheet(Sheet sheet, List<ResultDistributionDto> distribution) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Type resultat");
        header.createCell(1).setCellValue("Count");
        int i = 1;
        for (ResultDistributionDto d : distribution) {
            Row row = sheet.createRow(i++);
            row.createCell(0).setCellValue(String.valueOf(d.resultType()));
            row.createCell(1).setCellValue(d.count());
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void buildDailySheet(Sheet sheet, List<ReportingDailyBreakdownDto> daily) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("Transactions");
        header.createCell(2).setCellValue("Matching");
        header.createCell(3).setCellValue("Anomalies");
        header.createCell(4).setCellValue("SuccessRate");
        header.createCell(5).setCellValue("MontantBanque");
        header.createCell(6).setCellValue("MontantOperateur");
        header.createCell(7).setCellValue("Ecart");
        int i = 1;
        for (ReportingDailyBreakdownDto d : daily) {
            Row row = sheet.createRow(i++);
            row.createCell(0).setCellValue(String.valueOf(d.businessDate()));
            row.createCell(1).setCellValue(d.totalTransactions());
            row.createCell(2).setCellValue(d.matchingCount());
            row.createCell(3).setCellValue(d.anomalyCount());
            row.createCell(4).setCellValue(d.successRate().doubleValue());
            row.createCell(5).setCellValue(d.montantBanque().doubleValue());
            row.createCell(6).setCellValue(d.montantOperateur().doubleValue());
            row.createCell(7).setCellValue(d.ecart().doubleValue());
        }
    }

    private void buildDetailsSheet(Sheet sheet, List<ReportingTransactionDetailDto> details) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("TransactionKey");
        header.createCell(2).setCellValue("ResultType");
        header.createCell(3).setCellValue("Direction");
        header.createCell(4).setCellValue("BankStatus");
        header.createCell(5).setCellValue("OperatorStatus");
        header.createCell(6).setCellValue("BankAmount");
        header.createCell(7).setCellValue("OperatorAmount");
        header.createCell(8).setCellValue("AmountDiff");
        header.createCell(9).setCellValue("Reason");
        int i = 1;
        for (ReportingTransactionDetailDto d : details) {
            Row row = sheet.createRow(i++);
            row.createCell(0).setCellValue(String.valueOf(d.businessDate()));
            row.createCell(1).setCellValue(safe(d.transactionKey()));
            row.createCell(2).setCellValue(String.valueOf(d.resultType()));
            row.createCell(3).setCellValue(safe(d.direction()));
            row.createCell(4).setCellValue(safe(d.bankStatus()));
            row.createCell(5).setCellValue(safe(d.operatorStatus()));
            row.createCell(6).setCellValue(d.bankAmount() == null ? 0 : d.bankAmount().doubleValue());
            row.createCell(7).setCellValue(d.operatorAmount() == null ? 0 : d.operatorAmount().doubleValue());
            row.createCell(8).setCellValue(d.amountDifference() == null ? 0 : d.amountDifference().doubleValue());
            row.createCell(9).setCellValue(safe(d.reason()));
        }
    }

    private int writeKv(Sheet sheet, int rowIdx, String key, Object value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value == null ? "" : String.valueOf(value));
        return rowIdx + 1;
    }

    private float writeLine(PDPageContentStream stream, float x, float y, PDFont font, int size, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(text.length() > 140 ? text.substring(0, 140) : text);
        stream.endText();
        return y - (size + 4);
    }

    private BigDecimal rate(long count, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<ReportingRow> rows, java.util.function.Function<ReportingRow, BigDecimal> getter) {
        return rows.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long count(List<ReportingRow> rows, ReconciliationResultType type) {
        return rows.stream().filter(r -> r.result().getResultType() == type).count();
    }

    private boolean isFinanciallyRelevant(ReportingRow row) {
        return row.result().getResultType() != ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE
                && row.result().getResultType() != ReconciliationResultType.APPROVISIONNEMENT;
    }

    private boolean isOperatorOutOfScope(ReportingRow row) {
        return row.result().getResultType() == ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE;
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

    private boolean inRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private ReportWindow resolveWindow(ReportingPeriodType periodType, LocalDate referenceDate) {
        LocalDate ref = referenceDate == null ? LocalDate.now() : referenceDate;
        return switch (periodType) {
            case DAY -> new ReportWindow(ref, ref);
            case WEEK -> {
                LocalDate from = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate to = ref.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                yield new ReportWindow(from, to);
            }
            case MONTH -> {
                LocalDate from = ref.withDayOfMonth(1);
                LocalDate to = ref.withDayOfMonth(ref.lengthOfMonth());
                yield new ReportWindow(from, to);
            }
        };
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<ReportingRow> enrichByTransactionDate(List<ReconciliationResult> results, OperatorType channel, ReportWindow window) {
        Set<Long> bankIds = results.stream().map(ReconciliationResult::getBankTransactionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> operatorIds = results.stream().map(ReconciliationResult::getMoovTransactionId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, BankTransaction> bankById = bankTransactionRepository.findAllById(bankIds).stream()
                .collect(Collectors.toMap(BankTransaction::getId, t -> t, (a, b) -> a));

        Map<Long, LocalDate> bankDates = bankById.values().stream()
                .filter(t -> t.getTransactionDate() != null)
                .collect(Collectors.toMap(BankTransaction::getId, t -> t.getTransactionDate().toLocalDate(), (a, b) -> a));
        Map<Long, String> bankOperationTypes = new HashMap<>();
        for (BankTransaction transaction : bankById.values()) {
            String operationType = normalizeOperationType(String.valueOf(transaction.getOperationNature()));
            if (operationType != null) {
                bankOperationTypes.put(transaction.getId(), operationType);
            }
        }

        Map<Long, LocalDate> operatorDates = new HashMap<>();
        Map<Long, Boolean> operatorSuccessById = new HashMap<>();
        Map<Long, BigDecimal> operatorAmountById = new HashMap<>();
        Map<Long, String> operatorOperationTypes = new HashMap<>();
        if (channel == OperatorType.MOOV) {
            List<MoovTransaction> moovTransactions = moovTransactionRepository.findAllById(operatorIds);
            operatorDates.putAll(moovTransactions.stream()
                    .map(t -> Map.entry(t.getId(), t.getCompletionTime() != null ? t.getCompletionTime().toLocalDate() :
                            (t.getInitiationTime() != null ? t.getInitiationTime().toLocalDate() : null)))
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a)));
            operatorSuccessById.putAll(moovTransactions.stream()
                    .collect(Collectors.toMap(MoovTransaction::getId, t -> t.getTransactionStatusNormalized() == NormalizedMoovStatus.SUCCESS_MOOV, (a, b) -> a)));
            operatorAmountById.putAll(moovTransactions.stream()
                    .filter(t -> t.getAmount() != null)
                    .collect(Collectors.toMap(MoovTransaction::getId, MoovTransaction::getAmount, (a, b) -> a)));
            for (MoovTransaction transaction : moovTransactions) {
                String operationType = normalizeOperationType(String.valueOf(transaction.getTransactionType()));
                if (operationType != null) {
                    operatorOperationTypes.put(transaction.getId(), operationType);
                }
            }
        } else {
            List<OrangeTransaction> orangeTransactions = orangeTransactionRepository.findAllById(operatorIds);
            operatorDates.putAll(orangeTransactions.stream()
                    .filter(t -> t.getTransactionDateTime() != null)
                    .collect(Collectors.toMap(OrangeTransaction::getId, t -> t.getTransactionDateTime().toLocalDate(), (a, b) -> a)));
            operatorSuccessById.putAll(orangeTransactions.stream()
                    .collect(Collectors.toMap(OrangeTransaction::getId, t -> t.getTransactionStatusNormalized() == NormalizedOrangeStatus.SUCCESS_ORANGE, (a, b) -> a)));
            operatorAmountById.putAll(orangeTransactions.stream()
                    .filter(t -> t.getAmount() != null)
                    .collect(Collectors.toMap(OrangeTransaction::getId, OrangeTransaction::getAmount, (a, b) -> a)));
        }

        List<ReportingRow> enriched = new ArrayList<>();
        for (ReconciliationResult result : results) {
            LocalDate txDate = resolveTransactionDate(result, bankDates, operatorDates);
            if (inRange(txDate, window.from(), window.to())) {
                BankTransaction bank = result.getBankTransactionId() == null ? null : bankById.get(result.getBankTransactionId());
                boolean bankSuccess = bank != null && bank.getAllocationStatusNormalized() == NormalizedBankStatus.SUCCESS_BANK;
                boolean operatorSuccess = result.getMoovTransactionId() != null
                        && Boolean.TRUE.equals(operatorSuccessById.get(result.getMoovTransactionId()));
                BigDecimal bankAmount = bank != null && bank.getAmount() != null ? bank.getAmount() : result.getBankAmount();
                BigDecimal operatorAmount = result.getMoovTransactionId() == null
                        ? result.getMoovAmount()
                        : operatorAmountById.getOrDefault(result.getMoovTransactionId(), result.getMoovAmount());
                String operationType = resolveOperationType(result, bankOperationTypes, operatorOperationTypes);
                enriched.add(new ReportingRow(result, txDate, bankSuccess, operatorSuccess, bankAmount, operatorAmount, operationType));
            }
        }
        return enriched;
    }

    private String resolveOperationType(ReconciliationResult result,
                                        Map<Long, String> bankOperationTypes,
                                        Map<Long, String> operatorOperationTypes) {
        if (result.getMoovTransactionId() != null) {
            String operatorType = operatorOperationTypes.get(result.getMoovTransactionId());
            if (operatorType != null) {
                return operatorType;
            }
        }
        if (result.getBankTransactionId() != null) {
            String bankType = bankOperationTypes.get(result.getBankTransactionId());
            if (bankType != null) {
                return bankType;
            }
        }
        if (result.getBankTransactionId() != null && result.getMoovTransactionId() == null) {
            return "BANK_TO_WALLET";
        }
        if (result.getBankTransactionId() == null && result.getMoovTransactionId() != null) {
            return "WALLET_TO_BANK";
        }
        return null;
    }

    private String normalizeOperationType(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (value.equals("BANK_TO_WALLET") || value.equals("BANK_TO_MOOV")) {
            return "BANK_TO_WALLET";
        }
        if (value.equals("WALLET_TO_BANK") || value.equals("MOOV_TO_BANK")) {
            return "WALLET_TO_BANK";
        }
        return null;
    }

    private LocalDate resolveTransactionDate(ReconciliationResult result, Map<Long, LocalDate> bankDates, Map<Long, LocalDate> operatorDates) {
        if (result.getBankTransactionId() != null) {
            LocalDate bank = bankDates.get(result.getBankTransactionId());
            if (bank != null) {
                return bank;
            }
        }
        if (result.getMoovTransactionId() != null) {
            LocalDate operator = operatorDates.get(result.getMoovTransactionId());
            if (operator != null) {
                return operator;
            }
        }
        return result.getBusinessDate();
    }

    private List<String> buildExecutiveInsights(ReportingSummaryDto summary) {
        ReportingKpiDto k = summary.kpis();
        List<String> insights = new ArrayList<>();

        String trend = summary.periodType() == ReportingPeriodType.WEEK ? "hebdomadaire" : "mensuelle";
        insights.add("Performance " + trend + " : success rate a " + k.successRate() + "%, anomaly rate a " + k.anomalyRate() + "%.");

        if (k.anomalyRate().compareTo(BigDecimal.valueOf(20)) >= 0) {
            insights.add("Risque eleve : le taux d'anomalies depasse 20%. Prioriser le traitement DEBIT_A_TORT et ABSENTS.");
        } else if (k.anomalyRate().compareTo(BigDecimal.valueOf(10)) >= 0) {
            insights.add("Risque modere : renforcer le suivi journalier des anomalies pour reduire les ecarts.");
        } else {
            insights.add("Risque maitrise : niveau d'anomalie faible sur la periode.");
        }

        if (k.ecartGlobal().abs().compareTo(BigDecimal.ZERO) > 0) {
            insights.add("Ecart financier detecte (" + k.ecartGlobal() + "). Reconciliation manuelle recommandee sur les plus gros montants.");
        } else {
            insights.add("Aucun ecart financier global materialise sur la periode.");
        }

        if (k.picVolumeJournalier() != null && k.picVolumeJournalier().businessDate() != null) {
            insights.add("Pic d'activite observe le " + k.picVolumeJournalier().businessDate()
                    + " avec " + k.picVolumeJournalier().totalTransactions() + " transactions.");
        }

        insights.add("Actions recommandees: 1) corriger les anomalies critiques, 2) suivre les doublons/statuts inconnus, 3) valider les transactions a fort ecart.");
        return insights;
    }

    private record ReportWindow(LocalDate from, LocalDate to) {}

    private record ReportData(List<ReportingRow> rows) {}

    private record ReportingRow(
            ReconciliationResult result,
            LocalDate transactionDate,
            boolean bankSuccess,
            boolean operatorSuccess,
            BigDecimal bankAmount,
            BigDecimal operatorAmount,
            String operationType
    ) {}
}
