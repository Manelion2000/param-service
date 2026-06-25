package com.bakouan.app.service;

import com.bakouan.app.enums.ImportStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.security.IUserDetailsFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BankImportExportMoovFormatIT {

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
    private BankTransactionRepository bankTransactionRepository;

    @Test
    void shouldImportNewBankExportFormat() throws Exception {
        LocalDate businessDate = LocalDate.of(2026, 4, 18);
        Path filePath = Path.of("image", "exportMOOV18042026R.xls");
        assertTrue(Files.exists(filePath), "Fichier de test introuvable: " + filePath.toAbsolutePath());

        byte[] payload = Files.readAllBytes(filePath);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "exportMOOV18042026R.xls",
                "application/vnd.ms-excel",
                payload
        );

        FileImport bankImport = fileImportService.importFile(SourceType.BANQUE, OperatorType.MOOV, businessDate, file);

        assertNotNull(bankImport.getId());
        assertNotEquals(ImportStatus.FAILED, bankImport.getImportStatus());
        assertNotNull(bankImport.getValidRows());
        assertTrue(bankImport.getValidRows() > 0);

        List<BankTransaction> rows = bankTransactionRepository.findByFileImportId(bankImport.getId());
        assertFalse(rows.isEmpty());

        BankTransaction first = rows.get(0);
        assertNotNull(first.getTransactionId());
        assertNotNull(first.getAllocationStatusRaw());
        assertNotNull(first.getAllocationStatusNormalized());
        assertNotNull(first.getAmount());
        assertNotNull(first.getTransactionDate());
    }
}

