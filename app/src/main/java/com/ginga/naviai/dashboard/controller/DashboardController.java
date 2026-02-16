package com.ginga.naviai.dashboard.controller;

import com.ginga.naviai.dashboard.dto.ActivityResponse;
import com.ginga.naviai.dashboard.dto.DashboardSummaryResponse;
import com.ginga.naviai.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        DashboardSummaryResponse summary = dashboardService.getSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/activity")
    public ResponseEntity<ActivityResponse> getActivity(
            @org.springframework.web.bind.annotation.RequestParam("from") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @org.springframework.web.bind.annotation.RequestParam("to") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @org.springframework.web.bind.annotation.RequestParam(name = "range", required = false) String range
    ) {
        ActivityResponse resp = dashboardService.getActivity(from, to, range == null ? "week" : range);
        return ResponseEntity.ok(resp);
    }
}
