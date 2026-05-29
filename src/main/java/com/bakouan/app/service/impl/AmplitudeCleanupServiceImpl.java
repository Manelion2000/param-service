package com.bakouan.app.service.impl;

import com.bakouan.app.dto.AmplitudeCleanupResultDto;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.repositories.AmplitudeTransactionRepository;
import com.bakouan.app.repositories.FileImportRepository;
import com.bakouan.app.service.AmplitudeCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AmplitudeCleanupServiceImpl implements AmplitudeCleanupService {

    private final FileImportRepository fileImportRepository;
    private final AmplitudeTransactionRepository amplitudeTransactionRepository;

    @Override
    @Transactional
    public AmplitudeCleanupResultDto cleanupAll() {
        List<FileImport> imports = fileImportRepository.findBySourceType(SourceType.AMPLITUDE);
        if (imports.isEmpty()) {
            return new AmplitudeCleanupResultDto("ALL", null, null, 0, 0);
        }
        int deletedTransactions = 0;
        for (FileImport fileImport : imports) {
            Long importId = fileImport.getId();
            deletedTransactions += amplitudeTransactionRepository.countByFileImportId(importId);
            amplitudeTransactionRepository.deleteByFileImportId(importId);
        }
        fileImportRepository.deleteAll(imports);
        return new AmplitudeCleanupResultDto("ALL", null, null, imports.size(), deletedTransactions);
    }

    @Override
    @Transactional
    public AmplitudeCleanupResultDto cleanupDaily(LocalDate businessDate) {
        return cleanup("DAILY", businessDate, businessDate);
    }

    @Override
    @Transactional
    public AmplitudeCleanupResultDto cleanupRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom ne peut pas etre apres dateTo");
        }
        return cleanup("RANGE", dateFrom, dateTo);
    }

    @Override
    @Transactional
    public AmplitudeCleanupResultDto cleanupWeekly(LocalDate referenceDate) {
        LocalDate from = referenceDate.with(DayOfWeek.MONDAY);
        LocalDate to = referenceDate.with(DayOfWeek.SUNDAY);
        return cleanup("WEEKLY", from, to);
    }

    private AmplitudeCleanupResultDto cleanup(String mode, LocalDate dateFrom, LocalDate dateTo) {
        List<FileImport> imports = fileImportRepository.findBySourceTypeAndBusinessDateBetween(SourceType.AMPLITUDE, dateFrom, dateTo);
        if (imports.isEmpty()) {
            return new AmplitudeCleanupResultDto(mode, dateFrom, dateTo, 0, 0);
        }

        int deletedTransactions = 0;
        for (FileImport fileImport : imports) {
            Long importId = fileImport.getId();
            deletedTransactions += amplitudeTransactionRepository.countByFileImportId(importId);
            amplitudeTransactionRepository.deleteByFileImportId(importId);
        }
        fileImportRepository.deleteAll(imports);
        return new AmplitudeCleanupResultDto(mode, dateFrom, dateTo, imports.size(), deletedTransactions);
    }
}
