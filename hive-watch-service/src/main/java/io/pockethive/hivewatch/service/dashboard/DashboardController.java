package io.pockethive.hivewatch.service.dashboard;

import io.pockethive.hivewatch.service.api.DashboardDto;
import io.pockethive.hivewatch.service.api.DashboardEnvironmentDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
    private final DashboardQueryService dashboardQueryService;

    public DashboardController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/api/v1/dashboard")
    public DashboardDto dashboard() {
        return dashboardQueryService.getDashboard();
    }

    @GetMapping("/api/v1/dashboard/environments")
    public List<DashboardEnvironmentDto> list() {
        return dashboardQueryService.listEnvironments();
    }
}
