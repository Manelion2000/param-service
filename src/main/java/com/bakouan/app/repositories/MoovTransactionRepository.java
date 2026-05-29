package com.bakouan.app.repositories;

import com.bakouan.app.model.MoovTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

public interface MoovTransactionRepository extends JpaRepository<MoovTransaction, Long> {
    List<MoovTransaction> findByFileImportIdIn(Collection<Long> importIds);
    Page<MoovTransaction> findByFileImportId(Long importId, Pageable pageable);
    Page<MoovTransaction> findByFileImportIdAndTransactionTypeIgnoreCase(Long importId, String transactionType, Pageable pageable);
    int countByFileImportId(Long importId);
    void deleteByFileImportId(Long importId);

    @Query("select m.receiptNo from MoovTransaction m where m.receiptNo in :receiptNos")
    Set<String> findExistingReceiptNos(@Param("receiptNos") Collection<String> receiptNos);

    @Query("select m from MoovTransaction m where m.receiptNo in :receiptNos and m.fileImport.businessDate = :businessDate")
    List<MoovTransaction> findByReceiptNoInAndBusinessDate(@Param("receiptNos") Collection<String> receiptNos,
                                                           @Param("businessDate") java.time.LocalDate businessDate);

    @Query("select distinct m.fileImport.id from MoovTransaction m where m.completionTime >= :from and m.completionTime < :to")
    Set<Long> findImportIdsByCompletionTimeRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select coalesce(sum(m.amount), 0)
            from MoovTransaction m
            where m.transactionStatusNormalized = com.bakouan.app.enums.NormalizedMoovStatus.SUCCESS_MOOV
              and m.completionTime >= :from
              and m.completionTime < :to
            """)
    BigDecimal sumSuccessAmountByTransactionDateRange(@Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);

    @Query("""
            select count(m)
            from MoovTransaction m
            where m.transactionStatusNormalized = com.bakouan.app.enums.NormalizedMoovStatus.SUCCESS_MOOV
              and m.completionTime >= :from
              and m.completionTime < :to
            """)
    long countSuccessByTransactionDateRange(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);
}

