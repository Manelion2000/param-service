package com.bakouan.app.repositories;

import com.bakouan.app.model.BankTransaction;
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
import com.bakouan.app.enums.OperatorType;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    List<BankTransaction> findByFileImportIdIn(Collection<Long> importIds);
    List<BankTransaction> findByFileImportId(Long importId);
    Page<BankTransaction> findByFileImportId(Long importId, Pageable pageable);
    int countByFileImportId(Long importId);
    void deleteByFileImportId(Long importId);

    @Query("select b.transactionId from BankTransaction b where b.transactionId in :transactionIds")
    Set<String> findExistingTransactionIds(@Param("transactionIds") Collection<String> transactionIds);

    @Query("select distinct b.fileImport.id from BankTransaction b where b.transactionDate >= :from and b.transactionDate < :to")
    Set<Long> findImportIdsByTransactionDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select coalesce(sum(b.amount), 0)
            from BankTransaction b
            where b.allocationStatusNormalized = com.bakouan.app.enums.NormalizedBankStatus.SUCCESS_BANK
              and b.fileImport.operatorScope = :operator
              and b.transactionDate >= :from
              and b.transactionDate < :to
            """)
    BigDecimal sumSuccessAmountByTransactionDateRangeAndOperator(@Param("from") LocalDateTime from,
                                                                 @Param("to") LocalDateTime to,
                                                                 @Param("operator") OperatorType operator);

    @Query("""
            select count(b)
            from BankTransaction b
            where b.allocationStatusNormalized = com.bakouan.app.enums.NormalizedBankStatus.SUCCESS_BANK
              and b.fileImport.operatorScope = :operator
              and b.transactionDate >= :from
              and b.transactionDate < :to
            """)
    long countSuccessByTransactionDateRangeAndOperator(@Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to,
                                                       @Param("operator") OperatorType operator);
}

