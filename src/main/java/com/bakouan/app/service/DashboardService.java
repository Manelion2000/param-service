package com.bakouan.app.service;

import com.bakouan.app.dto.dashboard.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DashboardService {
    DashboardSummaryDto summary(DashboardFilterRequest filter);

    List<ResultDistributionDto> resultsDistribution(DashboardFilterRequest filter);

    DashboardAmountsDto amounts(DashboardFilterRequest filter);

    List<DashboardTimelinePointDto> timeline(DashboardFilterRequest filter);

    Page<TopAnomalyDto> topAnomalies(DashboardFilterRequest filter, Pageable pageable);

    DataQualityDto dataQuality(DashboardFilterRequest filter);
}

