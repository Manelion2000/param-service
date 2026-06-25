package com.bakouan.app.repositories;

import com.bakouan.app.dto.dashboard.AmountsView;
import com.bakouan.app.dto.dashboard.ResultTypeCountView;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.enums.ReconciliationResultType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long>, JpaSpecificationExecutor<ReconciliationResult> {
    Page<ReconciliationResult> findByRunId(Long runId, Pageable pageable);
    Page<ReconciliationResult> findByRunIdAndResultType(Long runId, ReconciliationResultType resultType, Pageable pageable);
    List<ReconciliationResult> findByRunIdIn(Collection<Long> runIds);

    @Query("""
            select r.resultType as resultType, count(r) as count
            from ReconciliationResult r
            where r.run.id in :runIds
              and ((:dateFrom is null and :dateTo is null) or r.businessDate between :dateFrom and :dateTo)
            group by r.resultType
            """)
    List<ResultTypeCountView> countByResultTypeForRuns(@Param("runIds") Collection<Long> runIds,
                                                       @Param("dateFrom") java.time.LocalDate dateFrom,
                                                       @Param("dateTo") java.time.LocalDate dateTo);

    @Query("""
            select coalesce(sum(r.bankAmount), 0) as bankTotal,
                   coalesce(sum(r.moovAmount), 0) as operatorTotal
            from ReconciliationResult r
            where r.run.id in :runIds
              and r.businessDate between :dateFrom and :dateTo
            """)
    AmountsView sumsForRuns(@Param("runIds") Collection<Long> runIds,
                            @Param("dateFrom") java.time.LocalDate dateFrom,
                            @Param("dateTo") java.time.LocalDate dateTo);

    @Query("""
            select r from ReconciliationResult r
            where r.run.id in :runIds
              and r.resultType <> com.bakouan.app.enums.ReconciliationResultType.MATCH_OK
              and r.resultType <> com.bakouan.app.enums.ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE
              and r.businessDate between :dateFrom and :dateTo
            order by case
                when r.resultType = com.bakouan.app.enums.ReconciliationResultType.DEBIT_A_TORT then 1
                when r.resultType = com.bakouan.app.enums.ReconciliationResultType.CREDIT_SANS_DEBIT then 2
                when r.resultType = com.bakouan.app.enums.ReconciliationResultType.MONTANT_DIFFERENT then 3
                when r.resultType = com.bakouan.app.enums.ReconciliationResultType.ABSENT_COTE_MOOV then 4
                when r.resultType = com.bakouan.app.enums.ReconciliationResultType.ABSENT_COTE_ORANGE then 4
                when r.resultType = com.bakouan.app.enums.ReconciliationResultType.ABSENT_COTE_BANQUE then 5
                else 6
            end asc, r.createdAt desc
            """)
    Page<ReconciliationResult> findTopAnomaliesForRuns(@Param("runIds") Collection<Long> runIds,
                                                       @Param("dateFrom") java.time.LocalDate dateFrom,
                                                       @Param("dateTo") java.time.LocalDate dateTo,
                                                       Pageable pageable);

    int countByRunId(Long runId);
    void deleteByRunId(Long runId);
}

