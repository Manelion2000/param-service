package com.bakouan.app.dto.dashboard;

public record ResultDistributionDto(
        DashboardResultTypeView resultType,
        long count
) {
}

