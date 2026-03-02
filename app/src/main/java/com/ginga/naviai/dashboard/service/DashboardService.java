package com.ginga.naviai.dashboard.service;

import com.ginga.naviai.dashboard.dto.ActivityResponse;
import com.ginga.naviai.dashboard.dto.DashboardSummaryResponse;

import java.time.LocalDate;

public interface DashboardService {
    DashboardSummaryResponse getSummary();

    ActivityResponse getActivity(LocalDate from, LocalDate to, String range);
}
