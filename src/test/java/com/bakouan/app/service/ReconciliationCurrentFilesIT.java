package com.bakouan.app.service;

import com.bakouan.app.dto.ReconciliationRunRequest;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.dto.ReconciliationSummaryDto;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.security.IUserDetailsFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.time.LocalDate;

@SpringBootTest
@ActiveProfiles("test")
class ReconciliationCurrentFilesIT {

    @TestConfiguration
    static class TestBeans {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @MockBean
    private IUserDetailsFacade userDetailsFacade;

    @Autowired
    private FileImportService fileImportService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Test
    void shouldRunReconciliationOnCurrentFilesAndPrintSummary() throws Exception {
        LocalDate businessDate = LocalDate.of(2026, 3, 9);

        FileImport moovImport;
        try (InputStream in = new ClassPathResource("fichier/Moov Money - BSIC Daily Reconciliation_2026-03-09.xls").getInputStream()) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "Moov Money - BSIC Daily Reconciliation_2026-03-09.xls",
                    "application/vnd.ms-excel",
                    in
            );
            moovImport = fileImportService.importFile(SourceType.MOOV, null, businessDate, file);
        }

        FileImport bankImport;
        try (InputStream in = new ClassPathResource("fichier/Transfert Banque vers Wallet Moov 09032026.xlsx").getInputStream()) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "Transfert Banque vers Wallet Moov 09032026.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    in
            );
            bankImport = fileImportService.importFile(SourceType.BANQUE, OperatorType.MOOV, businessDate, file);
        }

        ReconciliationRun run = reconciliationService.run(new ReconciliationRunRequest(
                businessDate,
                null,
                null,
                OperatorType.MOOV,
                null,
                null,
                null,
                "CURRENT-FILES-2026-03-09"
        ));

        ReconciliationSummaryDto summary = reconciliationService.summary(run.getId());

        System.out.println("IMPORT_MOOV total=" + moovImport.getTotalRows() + " valid=" + moovImport.getValidRows() + " invalid=" + moovImport.getInvalidRows() + " status=" + moovImport.getImportStatus());
        System.out.println("IMPORT_BANK total=" + bankImport.getTotalRows() + " valid=" + bankImport.getValidRows() + " invalid=" + bankImport.getInvalidRows() + " status=" + bankImport.getImportStatus());
        System.out.println("SUMMARY totalBank=" + summary.totalBank()
                + " totalMoov=" + summary.totalMoov()
                + " totalMatchOk=" + summary.totalMatchOk()
                + " totalDebitATort=" + summary.totalDebitATort()
                + " totalCreditSansDebit=" + summary.totalCreditSansDebit()
                + " totalAbsentBanque=" + summary.totalAbsentBanque()
                + " totalAbsentMoov=" + summary.totalAbsentMoov()
                + " totalMontantDifferent=" + summary.totalMontantDifferent()
                + " totalDoublons=" + summary.totalDoublons()
                + " montantGlobalBanque=" + summary.montantGlobalBanque()
                + " montantGlobalMoov=" + summary.montantGlobalMoov()
                + " ecartGlobal=" + summary.ecartGlobal());

        printType(run.getId(), ReconciliationResultType.MATCH_OK);
        printType(run.getId(), ReconciliationResultType.ECHEC_DES_DEUX_COTES);
        printType(run.getId(), ReconciliationResultType.ABSENT_COTE_BANQUE);
        printType(run.getId(), ReconciliationResultType.ABSENT_COTE_MOOV);
    }

    private void printType(Long runId, ReconciliationResultType type) {
        long count = reconciliationService.resultsByType(runId, type, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        System.out.println("TYPE " + type.name() + "=" + count);
        for (ReconciliationResult row : reconciliationService.resultsByType(runId, type, org.springframework.data.domain.Pageable.unpaged()).getContent().stream().limit(3).toList()) {
            System.out.println(" - key=" + row.getTransactionKey()
                    + " bankStatus=" + row.getBankStatusRaw()
                    + " moovStatus=" + row.getMoovStatusRaw()
                    + " bankAmount=" + row.getBankAmount()
                    + " moovAmount=" + row.getMoovAmount()
                    + " diff=" + row.getAmountDifference());
        }
    }
}
