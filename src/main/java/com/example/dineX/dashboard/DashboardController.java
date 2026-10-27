package com.example.dineX.dashboard;

import com.example.dineX.dashboard.DashboardService.HotelDashboard;
import com.example.dineX.dashboard.DashboardService.PlatformDashboard;
import com.example.dineX.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboards;

    public DashboardController(DashboardService dashboards) {
        this.dashboards = dashboards;
    }

    @GetMapping("/api/v1/hotel/dashboard")
    public HotelDashboard hotel(@AuthenticationPrincipal AuthUser user) {
        return dashboards.hotel(user.hotelId());
    }

    @GetMapping("/api/v1/admin/dashboard")
    public PlatformDashboard platform() {
        return dashboards.platform();
    }
}
