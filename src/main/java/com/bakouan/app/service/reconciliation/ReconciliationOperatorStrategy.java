package com.bakouan.app.service.reconciliation;

import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.ReconciliationRun;

import java.time.LocalDate;
import java.util.List;

public interface ReconciliationOperatorStrategy {
    SourceType operatorSourceType();
    List<FileImport> findImports(LocalDate from, LocalDate to);
    void reconcile(ReconciliationRun run, List<BankTransaction> bankRows, List<FileImport> operatorImports);
}
