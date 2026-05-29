package com.bakouan.app.service;

import com.bakouan.app.enums.ImportStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.FileImport;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class FileImportDeduplicationIT {

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

    @Test
    void shouldSkipAlreadyImportedBankTransactions() throws Exception {
        LocalDate businessDate = LocalDate.of(2026, 3, 9);
        String filename = "Transfert Banque vers Wallet Moov 09032026.xlsx";

        FileImport firstImport = importFromClasspath(filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", businessDate, SourceType.BANQUE);
        FileImport secondImport = importFromClasspath(filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", businessDate, SourceType.BANQUE);

        assertTrue(firstImport.getValidRows() > 0);
        assertEquals(firstImport.getTotalRows(), secondImport.getTotalRows());
        assertEquals(0, secondImport.getValidRows());
        assertEquals(secondImport.getTotalRows(), secondImport.getInvalidRows());
        assertEquals(ImportStatus.PARTIAL_SUCCESS, secondImport.getImportStatus());
    }

    private FileImport importFromClasspath(String path, String contentType, LocalDate businessDate, SourceType sourceType) throws Exception {
        try (InputStream in = new ClassPathResource("fichier/" + path).getInputStream()) {
            MockMultipartFile file = new MockMultipartFile("file", path, contentType, in);
            return fileImportService.importFile(sourceType, sourceType == SourceType.BANQUE ? OperatorType.MOOV : null, businessDate, file);
        }
    }
}
