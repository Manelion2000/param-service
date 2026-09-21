package com.bakouan.app.service.impl;

import com.bakouan.app.dto.CompensationDailyDto;
import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.MoovTransactionRepository;
import com.bakouan.app.repositories.OrangeTransactionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CompensationServiceImplTest {

    private final BankTransactionRepository bankTransactionRepository = Mockito.mock(BankTransactionRepository.class);
    private final MoovTransactionRepository moovTransactionRepository = Mockito.mock(MoovTransactionRepository.class);
    private final OrangeTransactionRepository orangeTransactionRepository = Mockito.mock(OrangeTransactionRepository.class);
    private final CompensationServiceImpl service = new CompensationServiceImpl(
            bankTransactionRepository,
            moovTransactionRepository,
            orangeTransactionRepository
    );

    @Test
    void shouldExcludeGeneratedAndIssuedBankStatusesFromMoovCompensationKpi() {
        LocalDate businessDate = LocalDate.of(2026, 3, 9);
        LocalDateTime from = businessDate.atStartOfDay();
        LocalDateTime to = businessDate.plusDays(1).atStartOfDay();
        when(bankTransactionRepository.findByAllocationStatusAndTransactionDateRangeAndOperator(
                NormalizedBankStatus.SUCCESS_BANK,
                from,
                to,
                OperatorType.MOOV
        )).thenReturn(List.of(
                bank("Alloue", "100.00"),
                bank("PaymentIssued", "200.00"),
                bank("Payment Issued", "300.00"),
                bank("Paiement genere", "400.00"),
                bank("Payement genere", "500.00"),
                bank("PayementIssuer", "600.00")
        ));
        when(moovTransactionRepository.countSuccessByTransactionDateRange(from, to)).thenReturn(1L);
        when(moovTransactionRepository.sumSuccessAmountByTransactionDateRange(from, to)).thenReturn(new BigDecimal("100.00"));
        when(moovTransactionRepository.findClosingBalancesByCompletionTimeRange(eq(from), eq(to), any(Pageable.class)))
                .thenReturn(List.of());

        List<CompensationDailyDto> rows = service.daily(businessDate, businessDate, OperatorType.MOOV);

        assertThat(rows).hasSize(1);
        CompensationDailyDto row = rows.get(0);
        assertThat(row.bankSuccessCount()).isEqualTo(1);
        assertThat(row.bankSuccessAmount()).isEqualByComparingTo("100.00");
        assertThat(row.operatorSuccessCount()).isEqualTo(1);
        assertThat(row.operatorSuccessAmount()).isEqualByComparingTo("100.00");
        assertThat(row.difference()).isEqualByComparingTo("0.00");
        assertThat(row.decision()).isEqualTo("OK_COMPENSATION");
    }

    @Test
    void shouldExcludeGeneratedAndIssuedBankStatusesFromOrangeCompensationKpi() {
        LocalDate businessDate = LocalDate.of(2026, 3, 9);
        LocalDateTime from = businessDate.atStartOfDay();
        LocalDateTime to = businessDate.plusDays(1).atStartOfDay();
        when(bankTransactionRepository.findByAllocationStatusAndTransactionDateRangeAndOperator(
                NormalizedBankStatus.SUCCESS_BANK,
                from,
                to,
                OperatorType.ORANGE
        )).thenReturn(List.of(
                bank("Alloue", "150.00"),
                bank("PaymentIssued", "250.00"),
                bank("Paiement genere", "350.00")
        ));
        when(orangeTransactionRepository.countSuccessByTransactionDateRange(from, to)).thenReturn(1L);
        when(orangeTransactionRepository.sumSuccessAmountByTransactionDateRange(from, to)).thenReturn(new BigDecimal("150.00"));

        List<CompensationDailyDto> rows = service.daily(businessDate, businessDate, OperatorType.ORANGE);

        assertThat(rows).hasSize(1);
        CompensationDailyDto row = rows.get(0);
        assertThat(row.bankSuccessCount()).isEqualTo(1);
        assertThat(row.bankSuccessAmount()).isEqualByComparingTo("150.00");
        assertThat(row.operatorSuccessCount()).isEqualTo(1);
        assertThat(row.operatorSuccessAmount()).isEqualByComparingTo("150.00");
        assertThat(row.difference()).isEqualByComparingTo("0.00");
        assertThat(row.decision()).isEqualTo("OK_COMPENSATION");
    }

    private BankTransaction bank(String status, String amount) {
        return BankTransaction.builder()
                .allocationStatusRaw(status)
                .allocationStatusNormalized(NormalizedBankStatus.SUCCESS_BANK)
                .amount(new BigDecimal(amount))
                .build();
    }
}
