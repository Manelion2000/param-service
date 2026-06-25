package com.bakouan.app.repositories;

import com.bakouan.app.model.AmplitudeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;

public interface AmplitudeTransactionRepository extends JpaRepository<AmplitudeTransaction, Long> {
    int countByFileImportId(Long importId);
    void deleteByFileImportId(Long importId);
    List<AmplitudeTransaction> findByFileImportIdIn(Collection<Long> importIds);
    List<AmplitudeTransaction> findByOperationDateGreaterThanEqualAndOperationDateLessThan(LocalDateTime from, LocalDateTime to);
    boolean existsByOperationReference(String operationReference);
}
