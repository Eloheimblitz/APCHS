package com.airpollution.survey.controller;

import com.airpollution.survey.dto.DashboardSummaryResponse;
import com.airpollution.survey.service.DashboardService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@RequestParam Map<String, String> filters, Authentication authentication) {
        return dashboardService.summary(filters, authentication);
    }
}
