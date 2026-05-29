package com.bakouan.app.service.impl;

import com.bakouan.app.config.RetentionProperties;
import com.bakouan.app.dto.RetentionExecutionDto;
import com.bakouan.app.enums.DataRetentionMode;
import com.bakouan.app.service.ReconciliationRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationRetentionServiceImpl implements ReconciliationRetentionService {

    private final JdbcTemplate jdbcTemplate;
    private final RetentionProperties retentionProperties;
    private final PlatformTransactionManager transactionManager;

    @Override
    public RetentionExecutionDto executeRetentionWithDefaultCutoff() {
        return executeRetentionWithDefaultCutoff(null);
    }

    @Override
    public RetentionExecutionDto executeRetentionWithDefaultCutoff(DataRetentionMode modeOverride) {
        LocalDate cutoff = LocalDate.now().minusDays(retentionProperties.getKeepDays());
        return executeRetention(cutoff, modeOverride);
    }

    @Override
    public RetentionExecutionDto executeRetention(LocalDate cutoffDateExclusive) {
        return executeRetention(cutoffDateExclusive, null);
    }

    @Override
    public RetentionExecutionDto executeRetention(LocalDate cutoffDateExclusive, DataRetentionMode modeOverride) {
        DataRetentionMode mode = modeOverride != null ? modeOverride : retentionProperties.getMode();
        long archivedResults = 0;
        long archivedRuns = 0;
        long purgedResults = 0;
        long purgedRuns = 0;

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        int batchSize = Math.max(1, retentionProperties.getBatchSize());

        if (mode == DataRetentionMode.ARCHIVE_AND_PURGE) {
            archivedResults = processByBatch(batchSize, () -> tx.execute(status -> archiveResultsBatch(cutoffDateExclusive, batchSize)));
            purgedResults = processByBatch(batchSize, () -> tx.execute(status -> purgeArchivedResultsBatch(cutoffDateExclusive, batchSize)));

            archivedRuns = processByBatch(batchSize, () -> tx.execute(status -> archiveRunsBatch(cutoffDateExclusive, batchSize)));
            purgedRuns = processByBatch(batchSize, () -> tx.execute(status -> purgeArchivedRunsBatch(cutoffDateExclusive, batchSize)));
        } else {
            purgedResults = processByBatch(batchSize, () -> tx.execute(status -> purgeResultsBatch(cutoffDateExclusive, batchSize)));
            purgedRuns = processByBatch(batchSize, () -> tx.execute(status -> purgeRunsBatch(cutoffDateExclusive, batchSize)));
        }

        RetentionExecutionDto dto = new RetentionExecutionDto(
                OffsetDateTime.now(),
                cutoffDateExclusive,
                mode,
                archivedResults,
                archivedRuns,
                purgedResults,
                purgedRuns
        );
        log.info("Retention executed: {}", dto);
        return dto;
    }

    @Scheduled(cron = "${app.retention.cron:0 30 2 * * *}")
    public void scheduledRetention() {
        if (!retentionProperties.isEnabled()) {
            return;
        }
        executeRetentionWithDefaultCutoff();
    }

    private long processByBatch(int batchSize, BatchOperation operation) {
        long total = 0;
        while (true) {
            Integer changed = operation.apply();
            int step = changed == null ? 0 : changed;
            total += step;
            if (step < batchSize) {
                break;
            }
        }
        return total;
    }

    private int archiveResultsBatch(LocalDate cutoffDateExclusive, int batchSize) {
        return jdbcTemplate.update("""
                INSERT INTO reconciliation_result_archive(
                    source_result_id, run_id, business_date, transaction_key, result_type,
                    bank_transaction_id, moov_transaction_id, bank_status_raw, moov_status_raw,
                    bank_amount, moov_amount, amount_difference, reason, created_at, archived_at
                )
                SELECT
                    rr.id, rr.run_id, rr.business_date, rr.transaction_key, rr.result_type,
                    rr.bank_transaction_id, rr.moov_transaction_id, rr.bank_status_raw, rr.moov_status_raw,
                    rr.bank_amount, rr.moov_amount, rr.amount_difference, rr.reason, rr.created_at, now()
                FROM reconciliation_result rr
                JOIN reconciliation_run r ON r.id = rr.run_id
                WHERE COALESCE(rr.business_date, r.business_date_from, DATE(r.started_at)) <= ?
                  AND NOT EXISTS (
                    SELECT 1 FROM reconciliation_result_archive a WHERE a.source_result_id = rr.id
                  )
                ORDER BY rr.id
                LIMIT ?
                """, Date.valueOf(cutoffDateExclusive), batchSize);
    }

    private int purgeArchivedResultsBatch(LocalDate cutoffDateExclusive, int batchSize) {
        return jdbcTemplate.update("""
                WITH to_delete AS (
                    SELECT rr.id
                    FROM reconciliation_result rr
                    JOIN reconciliation_run r ON r.id = rr.run_id
                    WHERE COALESCE(rr.business_date, r.business_date_from, DATE(r.started_at)) <= ?
                      AND EXISTS (
                        SELECT 1 FROM reconciliation_result_archive a WHERE a.source_result_id = rr.id
                      )
                    ORDER BY rr.id
                    LIMIT ?
                )
                DELETE FROM reconciliation_result rr
                USING to_delete d
                WHERE rr.id = d.id
                """, Date.valueOf(cutoffDateExclusive), batchSize);
    }

    private int archiveRunsBatch(LocalDate cutoffDateExclusive, int batchSize) {
        return jdbcTemplate.update("""
                INSERT INTO reconciliation_run_archive(
                    source_run_id, label, business_date_from, business_date_to, bank_import_ids,
                    moov_import_ids, started_at, finished_at, status, summary_json, archived_at
                )
                SELECT
                    r.id, r.label, r.business_date_from, r.business_date_to, r.bank_import_ids,
                    r.moov_import_ids, r.started_at, r.finished_at, r.status, r.summary_json, now()
                FROM reconciliation_run r
                WHERE COALESCE(r.business_date_to, r.business_date_from, DATE(r.started_at)) <= ?
                  AND NOT EXISTS (
                    SELECT 1 FROM reconciliation_result rr WHERE rr.run_id = r.id
                  )
                  AND NOT EXISTS (
                    SELECT 1 FROM reconciliation_run_archive a WHERE a.source_run_id = r.id
                  )
                ORDER BY r.id
                LIMIT ?
                """, Date.valueOf(cutoffDateExclusive), batchSize);
    }

    private int purgeArchivedRunsBatch(LocalDate cutoffDateExclusive, int batchSize) {
        return jdbcTemplate.update("""
                WITH to_delete AS (
                    SELECT r.id
                    FROM reconciliation_run r
                    WHERE COALESCE(r.business_date_to, r.business_date_from, DATE(r.started_at)) <= ?
                      AND NOT EXISTS (
                        SELECT 1 FROM reconciliation_result rr WHERE rr.run_id = r.id
                      )
                      AND EXISTS (
                        SELECT 1 FROM reconciliation_run_archive a WHERE a.source_run_id = r.id
                      )
                    ORDER BY r.id
                    LIMIT ?
                )
                DELETE FROM reconciliation_run r
                USING to_delete d
                WHERE r.id = d.id
                """, Date.valueOf(cutoffDateExclusive), batchSize);
    }

    private int purgeResultsBatch(LocalDate cutoffDateExclusive, int batchSize) {
        return jdbcTemplate.update("""
                WITH to_delete AS (
                    SELECT rr.id
                    FROM reconciliation_result rr
                    JOIN reconciliation_run r ON r.id = rr.run_id
                    WHERE COALESCE(rr.business_date, r.business_date_from, DATE(r.started_at)) <= ?
                    ORDER BY rr.id
                    LIMIT ?
                )
                DELETE FROM reconciliation_result rr
                USING to_delete d
                WHERE rr.id = d.id
                """, Date.valueOf(cutoffDateExclusive), batchSize);
    }

    private int purgeRunsBatch(LocalDate cutoffDateExclusive, int batchSize) {
        return jdbcTemplate.update("""
                WITH to_delete AS (
                    SELECT r.id
                    FROM reconciliation_run r
                    WHERE COALESCE(r.business_date_to, r.business_date_from, DATE(r.started_at)) <= ?
                      AND NOT EXISTS (
                        SELECT 1 FROM reconciliation_result rr WHERE rr.run_id = r.id
                      )
                    ORDER BY r.id
                    LIMIT ?
                )
                DELETE FROM reconciliation_run r
                USING to_delete d
                WHERE r.id = d.id
                """, Date.valueOf(cutoffDateExclusive), batchSize);
    }

    @FunctionalInterface
    private interface BatchOperation {
        Integer apply();
    }
}
