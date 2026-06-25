package com.bakouan.app.controller;

import com.bakouan.app.dto.ImportDeletionResult;
import com.bakouan.app.dto.ImportBulkDeletionResult;
import com.bakouan.app.dto.ImportDeletionPreviewResult;
import com.bakouan.app.dto.ImportFullDeletionResult;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.service.FileImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final FileImportService fileImportService;

    @PostMapping("/bank")
    public FileImport importBank(@RequestParam("file") MultipartFile file,
                                 @RequestParam("operator") OperatorType operator,
                                 @RequestParam("businessDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return fileImportService.importFile(SourceType.BANQUE, operator, businessDate, file);
    }

    @PostMapping("/moov")
    public FileImport importMoov(@RequestParam("file") MultipartFile file,
                                 @RequestParam("businessDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return fileImportService.importFile(SourceType.MOOV, null, businessDate, file);
    }

    @PostMapping("/orange")
    public FileImport importOrange(@RequestParam("file") MultipartFile file,
                                   @RequestParam("businessDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return fileImportService.importFile(SourceType.ORANGE, null, businessDate, file);
    }

    @PostMapping("/amplitude")
    public FileImport importAmplitude(@RequestParam("file") MultipartFile file,
                                      @RequestParam("businessDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return fileImportService.importFile(SourceType.AMPLITUDE, null, businessDate, file);
    }

    @GetMapping
    public Page<FileImport> list(Pageable pageable) {
        return fileImportService.list(pageable);
    }

    @GetMapping("/{id}")
    public FileImport get(@PathVariable Long id) {
        return fileImportService.get(id);
    }

    @GetMapping("/{id}/errors")
    public String errors(@PathVariable Long id) {
        return fileImportService.get(id).getErrorMessage();
    }

    @DeleteMapping("/{sourceType}/latest")
    public ImportDeletionResult deleteLatest(
            @PathVariable SourceType sourceType,
            @RequestParam(value = "confirmCascade", defaultValue = "false") boolean confirmCascade) {
        return fileImportService.deleteLatestImport(sourceType, confirmCascade);
    }

    @DeleteMapping("/{sourceType}")
    public ImportBulkDeletionResult deleteBySourceAndBusinessDate(
            @PathVariable SourceType sourceType,
            @RequestParam(value = "operator", required = false) OperatorType operator,
            @RequestParam("businessDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(value = "confirmCascade", defaultValue = "false") boolean confirmCascade) {
        return fileImportService.deleteImportsBySourceAndBusinessDate(
                sourceType,
                operator,
                businessDate,
                confirmCascade
        );
    }

    @DeleteMapping("/{sourceType}/all")
    public ImportFullDeletionResult deleteAllBySource(
            @PathVariable SourceType sourceType,
            @RequestParam(value = "operator", required = false) OperatorType operator,
            @RequestParam(value = "confirmCascade", defaultValue = "false") boolean confirmCascade) {
        return fileImportService.deleteAllImportsBySource(sourceType, operator, confirmCascade);
    }

    @GetMapping("/{sourceType}/preview-delete")
    public ImportDeletionPreviewResult previewDeleteBySourceAndBusinessDate(
            @PathVariable SourceType sourceType,
            @RequestParam(value = "operator", required = false) OperatorType operator,
            @RequestParam("businessDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return fileImportService.previewDeletionBySourceAndBusinessDate(sourceType, operator, businessDate);
    }
}

