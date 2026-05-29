package com.bakouan.app.controller;

import com.bakouan.app.dto.dashboard.*;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.service.DashboardService;
import com.bakouan.app.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/dashboard/reconciliation")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReportingService reportingService;

    @GetMapping("/summary")
    public DashboardSummaryDto summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam OperatorType channel,
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false) Long importId
    ) {
        return dashboardService.summary(validate(new DashboardFilterRequest(businessDate, dateFrom, dateTo, channel, runId, importId)));
    }

    @GetMapping("/results-distribution")
    public List<ResultDistributionDto> resultsDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam OperatorType channel,
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false) Long importId
    ) {
        return dashboardService.resultsDistribution(validate(new DashboardFilterRequest(businessDate, dateFrom, dateTo, channel, runId, importId)));
    }

    @GetMapping("/amounts")
    public DashboardAmountsDto amounts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam OperatorType channel,
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false) Long importId
    ) {
        return dashboardService.amounts(validate(new DashboardFilterRequest(businessDate, dateFrom, dateTo, channel, runId, importId)));
    }

    @GetMapping("/timeline")
    public List<DashboardTimelinePointDto> timeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam OperatorType channel,
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false) Long importId
    ) {
        return dashboardService.timeline(validate(new DashboardFilterRequest(businessDate, dateFrom, dateTo, channel, runId, importId)));
    }

    @GetMapping("/top-anomalies")
    public Page<TopAnomalyDto> topAnomalies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam OperatorType channel,
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false) Long importId,
            Pageable pageable
    ) {
        return dashboardService.topAnomalies(validate(new DashboardFilterRequest(businessDate, dateFrom, dateTo, channel, runId, importId)), pageable);
    }

    @GetMapping("/data-quality")
    public DataQualityDto dataQuality(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam OperatorType channel,
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false) Long importId
    ) {
        return dashboardService.dataQuality(validate(new DashboardFilterRequest(businessDate, dateFrom, dateTo, channel, runId, importId)));
    }

    @GetMapping("/reporting/summary")
    public ReportingSummaryDto reportingSummary(
            @RequestParam ReportingPeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam OperatorType channel
    ) {
        return reportingService.buildSummary(periodType, referenceDate, channel);
    }

    @GetMapping("/reporting/export/excel")
    public ResponseEntity<byte[]> exportReportingExcel(
            @RequestParam ReportingPeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam OperatorType channel
    ) {
        byte[] content = reportingService.exportExcel(periodType, referenceDate, channel);
        String filename = "reporting-" + channel.name().toLowerCase() + "-" + periodType.name().toLowerCase() + "-" + referenceDate + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @GetMapping("/reporting/export/pdf")
    public ResponseEntity<byte[]> exportReportingPdf(
            @RequestParam ReportingPeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam OperatorType channel
    ) {
        byte[] content = reportingService.exportPdf(periodType, referenceDate, channel);
        String filename = "reporting-" + channel.name().toLowerCase() + "-" + periodType.name().toLowerCase() + "-" + referenceDate + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    private DashboardFilterRequest validate(DashboardFilterRequest request) {
        if (request.channel() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "channel est obligatoire");
        }
        boolean hasBusinessDate = request.businessDate() != null;
        boolean hasRange = request.dateFrom() != null && request.dateTo() != null;
        if (hasBusinessDate && (request.dateFrom() != null || request.dateTo() != null)) {
            throw new ResponseStatusException(BAD_REQUEST, "Utiliser soit businessDate, soit dateFrom+dateTo");
        }
        if ((request.dateFrom() != null && request.dateTo() == null) || (request.dateFrom() == null && request.dateTo() != null)) {
            throw new ResponseStatusException(BAD_REQUEST, "dateFrom et dateTo sont obligatoires ensemble");
        }
        if (request.dateFrom() != null && request.dateFrom().isAfter(request.dateTo())) {
            throw new ResponseStatusException(BAD_REQUEST, "dateFrom doit etre <= dateTo");
        }
        return request;
    }
}
