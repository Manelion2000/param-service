package com.bakouan.app.dto.dashboard;

public record DashboardTimelinePointDto(
        String hour,
        long totalTransactions,
        long anomalies
) {
}

