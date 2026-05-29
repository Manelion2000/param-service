package com.bakouan.app.controller;

import com.bakouan.app.dto.RetentionExecutionDto;
import com.bakouan.app.enums.DataRetentionMode;
import com.bakouan.app.service.ReconciliationRetentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reconciliations/retention")
@RequiredArgsConstructor
public class ReconciliationRetentionController {

    private final ReconciliationRetentionService retentionService;

    @PostMapping("/run")
    public RetentionExecutionDto runRetention(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cutoffDateExclusive,
            @RequestParam(required = false) Integer keepDays,
            @RequestParam(required = false) DataRetentionMode mode
    ) {
        if (cutoffDateExclusive != null) {
            return retentionService.executeRetention(cutoffDateExclusive, mode);
        }
        if (keepDays != null && keepDays > 0) {
            return retentionService.executeRetention(LocalDate.now().minusDays(keepDays), mode);
        }
        return retentionService.executeRetentionWithDefaultCutoff(mode);
    }
}
