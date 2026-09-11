package com.fairshare.admin.controller;

import com.fairshare.admin.dto.DashboardStatsResponse;
import com.fairshare.admin.dto.RumMetricRequest;
import com.fairshare.admin.service.RumAnalyticsService;
import com.fairshare.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin & Analytics", description = "Real-Time User Monitoring (RUM) and platform dashboard statistics")
public class AdminController {

    private final RumAnalyticsService analyticsService;

    public AdminController(RumAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform overview and onboarding journey analytics")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        DashboardStatsResponse stats = analyticsService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @PostMapping("/rum")
    @Operation(summary = "Ingest real-time user monitoring (RUM) performance or navigation metric")
    public ResponseEntity<ApiResponse<Void>> recordRumMetric(@RequestBody RumMetricRequest request) {
        analyticsService.recordRumEvent(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Metric recorded"));
    }
}
