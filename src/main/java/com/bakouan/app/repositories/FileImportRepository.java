package com.bakouan.app.repositories;

import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.enums.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FileImportRepository extends JpaRepository<FileImport, Long> {
    List<FileImport> findBySourceTypeAndBusinessDate(SourceType sourceType, LocalDate businessDate);
    List<FileImport> findBySourceType(SourceType sourceType);
    List<FileImport> findBySourceTypeAndOperatorScope(SourceType sourceType, OperatorType operatorScope);
    List<FileImport> findBySourceTypeAndBusinessDateBetween(SourceType sourceType, LocalDate from, LocalDate to);
    List<FileImport> findBySourceTypeAndOperatorScopeAndBusinessDateBetween(SourceType sourceType, OperatorType operatorScope, LocalDate from, LocalDate to);
    @Query("""
            select f from FileImport f
            where f.sourceType = :sourceType
              and f.businessDate = :businessDate
              and f.checksum = :checksum
              and ((:operatorScope is null and f.operatorScope is null) or f.operatorScope = :operatorScope)
            """)
    Optional<FileImport> findDuplicateByScopeAndChecksum(@Param("sourceType") SourceType sourceType,
                                                         @Param("operatorScope") OperatorType operatorScope,
                                                         @Param("businessDate") LocalDate businessDate,
                                                         @Param("checksum") String checksum);
    Optional<FileImport> findTopBySourceTypeOrderByImportedAtDescIdDesc(SourceType sourceType);
}

