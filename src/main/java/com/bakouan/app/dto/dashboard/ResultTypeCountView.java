package com.bakouan.app.dto.dashboard;

import com.bakouan.app.enums.ReconciliationResultType;

public interface ResultTypeCountView {
    ReconciliationResultType getResultType();

    long getCount();
}

