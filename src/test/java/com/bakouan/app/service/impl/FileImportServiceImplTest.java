package com.bakouan.app.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileImportServiceImplTest {

    @Test
    void shouldParseBankCdateWithoutTime() {
        FileImportServiceImpl service = service();

        LocalDateTime parsed = (LocalDateTime) ReflectionTestUtils.invokeMethod(
                service,
                "parseBankTransactionDate",
                Map.of("CDATE_", "18/04/26")
        );

        assertThat(parsed).isEqualTo(LocalDateTime.of(2026, 4, 18, 0, 0));
    }

    @Test
    void shouldExtractCarthagoBankToWalletFields() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "OPERATIONNATURE_", "BankToWallet",
                "ALLOCATIONSTATUS_", "Allocated",
                "AMOUNT_", "20000",
                "MSISDN_", "22672025452",
                "OPERATIONREFERENCE_", "20260038244"
        );

        String operationNature = (String) ReflectionTestUtils.invokeMethod(service, "resolveBankOperationNature", row);
        String status = (String) ReflectionTestUtils.invokeMethod(service, "extractBankStatus", row);
        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(service, "extractBankAmount", row);
        String phone = (String) ReflectionTestUtils.invokeMethod(service, "extractBankPhoneNumber", row);
        String reference = (String) ReflectionTestUtils.invokeMethod(service, "extractBankOperationReference", row);

        assertThat(operationNature).isEqualTo("BANK_TO_WALLET");
        assertThat(status).isEqualTo("Allocated");
        assertThat(amount).isEqualByComparingTo("20000");
        assertThat(phone).isEqualTo("22672025452");
        assertThat(reference).isEqualTo("20260038244");
    }

    @Test
    void shouldExtractCarthagoWalletToBankFields() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "OPERATIONNATURE_", "WalletToBank",
                "DEALLOCATIONSTATUS_", "Deallocated",
                "TXAMOUNT_", "47500",
                "AMOUNT_", "47500",
                "TXMSISDN_", "22672691176",
                "OPERATIONREFERENCE_", "20260038005",
                "TXDATE_", "17/04/26 10:58:13,364000000"
        );

        String operationNature = (String) ReflectionTestUtils.invokeMethod(service, "resolveBankOperationNature", row);
        String status = (String) ReflectionTestUtils.invokeMethod(service, "extractBankStatus", row);
        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(service, "extractBankAmount", row);
        String phone = (String) ReflectionTestUtils.invokeMethod(service, "extractBankPhoneNumber", row);
        String reference = (String) ReflectionTestUtils.invokeMethod(service, "extractBankOperationReference", row);
        LocalDateTime date = (LocalDateTime) ReflectionTestUtils.invokeMethod(service, "parseBankTransactionDate", row);

        assertThat(operationNature).isEqualTo("WALLET_TO_BANK");
        assertThat(status).isEqualTo("Deallocated");
        assertThat(amount).isEqualByComparingTo("47500");
        assertThat(phone).isEqualTo("22672691176");
        assertThat(reference).isEqualTo("20260038005");
        assertThat(date).isEqualTo(LocalDateTime.of(2026, 4, 17, 10, 58, 13));
    }

    @Test
    void shouldExtractOrangeCarthagoBankToWalletFields() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "OPERATIONNATURE_", "BankToWallet",
                "ALLOCATIONSTATUS_", "Allocated",
                "AMOUNT_", "10000",
                "MSISDN_", "76440349",
                "OPERATIONREFERENCE_", "20260038247",
                "TRANSACTIONID_", "177651-102530-151153",
                "MOBILEMONEYPROVIDERCODE_", "ORANGE_BF"
        );

        String operationNature = (String) ReflectionTestUtils.invokeMethod(service, "resolveBankOperationNature", row);
        String status = (String) ReflectionTestUtils.invokeMethod(service, "extractBankStatus", row);
        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(service, "extractBankAmount", row);
        String phone = (String) ReflectionTestUtils.invokeMethod(service, "extractBankPhoneNumber", row);
        String reference = (String) ReflectionTestUtils.invokeMethod(service, "extractBankOperationReference", row);
        String transactionId = (String) ReflectionTestUtils.invokeMethod(service, "extractBankTransactionId", row);

        assertThat(operationNature).isEqualTo("BANK_TO_WALLET");
        assertThat(status).isEqualTo("Allocated");
        assertThat(amount).isEqualByComparingTo("10000");
        assertThat(phone).isEqualTo("76440349");
        assertThat(reference).isEqualTo("20260038247");
        assertThat(transactionId).isEqualTo("177651-102530-151153");
    }

    @Test
    void shouldExtractOrangeCarthagoWalletToBankFields() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "OPERATIONNATURE_", "WalletToBank",
                "DEALLOCATIONSTATUS_", "Deallocated",
                "TXAMOUNT_", "70000",
                "AMOUNT_", "0",
                "TXMSISDN_", "74746675",
                "OPERATIONREFERENCE_", "20260038002",
                "TRANSACTIONID_", "178001-103150-596000",
                "TXDATE_", "17/04/26 10:31:50,596000000",
                "MOBILEMONEYPROVIDERCODE_", "ORANGE_BF"
        );

        String operationNature = (String) ReflectionTestUtils.invokeMethod(service, "resolveBankOperationNature", row);
        String status = (String) ReflectionTestUtils.invokeMethod(service, "extractBankStatus", row);
        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(service, "extractBankAmount", row);
        String phone = (String) ReflectionTestUtils.invokeMethod(service, "extractBankPhoneNumber", row);
        String reference = (String) ReflectionTestUtils.invokeMethod(service, "extractBankOperationReference", row);
        String transactionId = (String) ReflectionTestUtils.invokeMethod(service, "extractBankTransactionId", row);
        LocalDateTime date = (LocalDateTime) ReflectionTestUtils.invokeMethod(service, "parseBankTransactionDate", row);

        assertThat(operationNature).isEqualTo("WALLET_TO_BANK");
        assertThat(status).isEqualTo("Deallocated");
        assertThat(amount).isEqualByComparingTo("70000");
        assertThat(phone).isEqualTo("74746675");
        assertThat(reference).isEqualTo("20260038002");
        assertThat(transactionId).isEqualTo("178001-103150-596000");
        assertThat(date).isEqualTo(LocalDateTime.of(2026, 4, 17, 10, 31, 50));
    }

    @Test
    void shouldExtractMoovBankToWalletAmountFromWithdrawn() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "Details", "Transfer from Bank to Moov Money",
                "Withdrawn", "-10000.00",
                "Paid In", "0.00"
        );

        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(
                service,
                "extractMoovAmount",
                row,
                row.get("Details")
        );
        String transactionType = (String) ReflectionTestUtils.invokeMethod(
                service,
                "resolveMoovTransactionType",
                row.get("Details"),
                row
        );

        assertThat(amount).isEqualByComparingTo("10000.00");
        assertThat(transactionType).isEqualTo("BANK_TO_WALLET");
    }

    @Test
    void shouldExtractMoovWalletToBankAmountFromPaidIn() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "Details", "Transfer from Moov Money to Bank",
                "Withdrawn", "0.00",
                "Paid In", "25000.00"
        );

        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(
                service,
                "extractMoovAmount",
                row,
                row.get("Details")
        );
        String transactionType = (String) ReflectionTestUtils.invokeMethod(
                service,
                "resolveMoovTransactionType",
                row.get("Details"),
                row
        );

        assertThat(amount).isEqualByComparingTo("25000.00");
        assertThat(transactionType).isEqualTo("WALLET_TO_BANK");
    }

    @Test
    void shouldKeepLegacyMoovAmountFallbackOnWithdrawn() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "Withdrawn", "-1500.00",
                "Amount", "2000.00"
        );

        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(
                service,
                "extractMoovAmount",
                row,
                null
        );

        assertThat(amount).isEqualByComparingTo("1500.00");
    }

    @Test
    void shouldExtractAmplitudeBankToWalletAmountFromCredit() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "_c6", "VIRT DIGIT         2026003813",
                "_c9", "",
                "_c10", "75000"
        );

        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(
                service,
                "extractAmplitudeAmount",
                row
        );
        String direction = (String) ReflectionTestUtils.invokeMethod(
                service,
                "resolveAmplitudeDirection",
                row
        );

        assertThat(amount).isEqualByComparingTo("75000");
        assertThat(direction).isEqualTo("BANK_TO_WALLET");
    }

    @Test
    void shouldExtractAmplitudeWalletToBankAmountFromDebitWhenCreditIsBlank() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "_c6", "VIREMENT W         2026003813",
                "_c9", "98000",
                "_c10", ""
        );

        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(
                service,
                "extractAmplitudeAmount",
                row
        );
        String direction = (String) ReflectionTestUtils.invokeMethod(
                service,
                "resolveAmplitudeDirection",
                row
        );

        assertThat(amount).isEqualByComparingTo("98000");
        assertThat(direction).isEqualTo("WALLET_TO_BANK");
    }

    @Test
    void shouldPreferAmplitudeCreditAmountWhenCreditAndDebitArePresent() {
        FileImportServiceImpl service = service();
        Map<String, String> row = Map.of(
                "_c6", "Total mouvements",
                "_c9", "14427950",
                "_c10", "80964641"
        );

        BigDecimal amount = (BigDecimal) ReflectionTestUtils.invokeMethod(
                service,
                "extractAmplitudeAmount",
                row
        );
        String direction = (String) ReflectionTestUtils.invokeMethod(
                service,
                "resolveAmplitudeDirection",
                row
        );

        assertThat(amount).isEqualByComparingTo("80964641");
        assertThat(direction).isEqualTo("BANK_TO_WALLET");
    }

    private FileImportServiceImpl service() {
        return new FileImportServiceImpl(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
