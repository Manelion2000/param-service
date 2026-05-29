package com.bakouan.app.service;

import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.dto.ImportDeletionResult;
import com.bakouan.app.dto.ImportBulkDeletionResult;
import com.bakouan.app.dto.ImportFullDeletionResult;
import com.bakouan.app.dto.ImportDeletionPreviewResult;
import com.bakouan.app.enums.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface FileImportService {
    FileImport importFile(SourceType sourceType, OperatorType operatorScope, LocalDate businessDate, MultipartFile file);
    Page<FileImport> list(Pageable pageable);
    FileImport get(Long id);
    ImportDeletionResult deleteLatestImport(SourceType sourceType);
    ImportBulkDeletionResult deleteImportsBySourceAndBusinessDate(SourceType sourceType, OperatorType operatorScope, LocalDate businessDate);
    ImportFullDeletionResult deleteAllImportsBySource(SourceType sourceType, OperatorType operatorScope);
    ImportDeletionPreviewResult previewDeletionBySourceAndBusinessDate(SourceType sourceType, OperatorType operatorScope, LocalDate businessDate);
}

