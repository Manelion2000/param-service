package com.bakouan.app.controller;

import com.bakouan.app.dto.dashboard.DashboardSummaryDto;
import com.bakouan.app.dto.dashboard.DashboardPeriodType;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardControllerUnitTest {

    @Test
    void shouldReturnSummaryForValidFilters() {
        DashboardService service = Mockito.mock(DashboardService.class);
        DashboardController controller = new DashboardController(service);
        DashboardSummaryDto expected = new DashboardSummaryDto(
                DashboardPeriodType.DAY,
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 3, 11),
                OperatorType.ORANGE,
                86, 143, 86, 80, 5, 1, 2, 57, 0, 1, 0, 3, 0,
                new BigDecimal("93.02"), new BigDecimal("89.50"), new BigDecimal("6.98"),
                new BigDecimal("1200000"), new BigDecimal("1195000"),
                new BigDecimal("45000"), new BigDecimal("5000")
        );
        when(service.summary(any())).thenReturn(expected);

        DashboardSummaryDto actual = controller.summary(
                LocalDate.of(2026, 3, 11),
                null,
                null,
                OperatorType.ORANGE,
                null,
                null
        );

        assertThat(actual.channel()).isEqualTo(OperatorType.ORANGE);
        assertThat(actual.totalBank()).isEqualTo(86);
    }

    @Test
    void shouldRejectInvalidDateCombination() {
        DashboardService service = Mockito.mock(DashboardService.class);
        DashboardController controller = new DashboardController(service);

        assertThatThrownBy(() -> controller.summary(
                null,
                LocalDate.of(2026, 3, 10),
                null,
                OperatorType.MOOV,
                null,
                null
        )).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldAllowMissingDateWhenRunIdIsProvided() {
        DashboardService service = Mockito.mock(DashboardService.class);
        DashboardController controller = new DashboardController(service);
        DashboardSummaryDto expected = new DashboardSummaryDto(
                DashboardPeriodType.DAY,
                null,
                null,
                null,
                OperatorType.MOOV,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        when(service.summary(any())).thenReturn(expected);

        DashboardSummaryDto actual = controller.summary(
                null,
                null,
                null,
                OperatorType.MOOV,
                42L,
                null
        );

        assertThat(actual.channel()).isEqualTo(OperatorType.MOOV);
        verify(service).summary(any());
    }
}
