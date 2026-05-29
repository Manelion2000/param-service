package com.bakouan.app.dto.dashboard;

import com.bakouan.app.enums.OperatorType;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;

public record DashboardFilterRequest(
        LocalDate businessDate,
        LocalDate dateFrom,
        LocalDate dateTo,
        OperatorType channel,
        Long runId,
        Long importId
) {

    @AssertTrue(message = "channel est obligatoire")
    public boolean isChannelPresent() {
        return channel != null;
    }

    @AssertTrue(message = "Utiliser businessDate ou (dateFrom et dateTo)")
    public boolean isDateFilterValid() {
        boolean hasBusinessDate = businessDate != null;
        boolean hasRange = dateFrom != null && dateTo != null;
        return hasBusinessDate || hasRange;
    }

    @AssertTrue(message = "dateFrom doit etre <= dateTo")
    public boolean isRangeChronological() {
        if (dateFrom == null || dateTo == null) {
            return true;
        }
        return !dateFrom.isAfter(dateTo);
    }

    public LocalDate effectiveFrom() {
        return businessDate != null ? businessDate : dateFrom;
    }

    public LocalDate effectiveTo() {
        return businessDate != null ? businessDate : dateTo;
    }

    public DashboardPeriodType periodType() {
        return businessDate != null ? DashboardPeriodType.DAY : DashboardPeriodType.RANGE;
    }
}
