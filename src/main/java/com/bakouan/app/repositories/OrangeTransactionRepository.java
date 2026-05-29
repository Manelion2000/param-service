package com.bakouan.app.repositories;

import com.bakouan.app.model.OrangeTransaction;
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

public interface OrangeTransactionRepository extends JpaRepository<OrangeTransaction, Long> {
    List<OrangeTransaction> findByFileImportIdIn(Collection<Long> importIds);
    Page<OrangeTransaction> findByFileImportId(Long importId, Pageable pageable);
    List<OrangeTransaction> findByAliasBankAccountNumberIn(Collection<String> accountNumbers);
    @Query("select o from OrangeTransaction o where o.omTransactionId in :omTransactionIds and o.fileImport.businessDate = :businessDate")
    List<OrangeTransaction> findByOmTransactionIdInAndBusinessDate(@Param("omTransactionIds") Collection<String> omTransactionIds,
                                                                   @Param("businessDate") java.time.LocalDate businessDate);
    @Query("select o.omTransactionId from OrangeTransaction o where o.omTransactionId in :omTransactionIds")
    Set<String> findExistingOmTransactionIds(@Param("omTransactionIds") Collection<String> omTransactionIds);
    @Query("select distinct o.fileImport.id from OrangeTransaction o where o.transactionDateTime >= :from and o.transactionDateTime < :to")
    Set<Long> findImportIdsByTransactionDateTimeRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    int countByFileImportId(Long importId);
    void deleteByFileImportId(Long importId);

    @Query("""
            select coalesce(sum(o.amount), 0)
            from OrangeTransaction o
            where o.transactionStatusNormalized = com.bakouan.app.enums.NormalizedOrangeStatus.SUCCESS_ORANGE
              and o.transactionDateTime >= :from
              and o.transactionDateTime < :to
            """)
    BigDecimal sumSuccessAmountByTransactionDateRange(@Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);

    @Query("""
            select count(o)
            from OrangeTransaction o
            where o.transactionStatusNormalized = com.bakouan.app.enums.NormalizedOrangeStatus.SUCCESS_ORANGE
              and o.transactionDateTime >= :from
              and o.transactionDateTime < :to
            """)
    long countSuccessByTransactionDateRange(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);
}
