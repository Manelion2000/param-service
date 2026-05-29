package com.bakouan.app.dto.dashboard;

import java.math.BigDecimal;

public record DashboardAmountsDto(
        BigDecimal montantGlobalBanque,
        BigDecimal montantGlobalOperateur,
        BigDecimal montantAnomalies,
        BigDecimal ecartGlobal
) {
}

