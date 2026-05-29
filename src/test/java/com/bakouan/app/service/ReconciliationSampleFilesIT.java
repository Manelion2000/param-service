package com.bakouan.app.service;

import com.bakouan.app.dto.ReconciliationRunRequest;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.ReconciliationRun;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.bakouan.app.security.IUserDetailsFacade;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ReconciliationSampleFilesIT {

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
    void shouldImportSampleFilesAndRunReconciliation() throws Exception {
        LocalDate businessDate = LocalDate.of(2026, 3, 3);

        FileImport moovImport;
        try (InputStream in = new ClassPathResource("fichier/Moov Money - BSIC Daily Reconciliation_2026-03-04.xls").getInputStream()) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "Moov Money - BSIC Daily Reconciliation_2026-03-04.xls",
                    "application/vnd.ms-excel",
                    in
            );
            moovImport = fileImportService.importFile(SourceType.MOOV, null, businessDate, file);
        }

        FileImport bankImport;
        try (InputStream in = new ClassPathResource("fichier/operation_carthago_jasper.xls").getInputStream()) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "operation_carthago_jasper.xls",
                    "application/vnd.ms-excel",
                    in
            );
            bankImport = fileImportService.importFile(SourceType.BANQUE, OperatorType.MOOV, businessDate, file);
        }

        assertNotNull(moovImport.getId());
        assertNotNull(bankImport.getId());
        assertTrue(moovImport.getValidRows() != null && moovImport.getValidRows() > 0);
        assertTrue(bankImport.getValidRows() != null && bankImport.getValidRows() > 0);

        ReconciliationRun run = reconciliationService.run(new ReconciliationRunRequest(
                businessDate,
                null,
                null,
                OperatorType.MOOV,
                null,
                null,
                null,
                "IT-RUN-2026-03-03"
        ));

        assertNotNull(run.getId());
        assertNotNull(run.getSummaryJson());
    }
}
