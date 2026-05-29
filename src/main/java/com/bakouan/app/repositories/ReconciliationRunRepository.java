package com.bakouan.app.repositories;

import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.model.ReconciliationRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Long> {
    Page<ReconciliationRun> findByBusinessDateFromBetween(LocalDate dateFrom, LocalDate dateTo, Pageable pageable);
    Page<ReconciliationRun> findByOperator(OperatorType operator, Pageable pageable);
    Page<ReconciliationRun> findByOperatorAndBusinessDateFromBetween(OperatorType operator, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);

    @Query("""
            select r from ReconciliationRun r
            where r.businessDateFrom is not null
              and r.businessDateFrom <= :toDate
              and coalesce(r.businessDateTo, r.businessDateFrom) >= :fromDate
            """)
    Page<ReconciliationRun> findByBusinessDateOverlap(@Param("fromDate") LocalDate fromDate,
                                                      @Param("toDate") LocalDate toDate,
                                                      Pageable pageable);

    @Query("""
            select r from ReconciliationRun r
            where r.operator = :operator
              and r.businessDateFrom is not null
              and r.businessDateFrom <= :toDate
              and coalesce(r.businessDateTo, r.businessDateFrom) >= :fromDate
            """)
    Page<ReconciliationRun> findByOperatorAndBusinessDateOverlap(@Param("operator") OperatorType operator,
                                                                  @Param("fromDate") LocalDate fromDate,
                                                                  @Param("toDate") LocalDate toDate,
                                                                  Pageable pageable);
}

