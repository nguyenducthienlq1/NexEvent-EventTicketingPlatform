package com.nexevent.nexevent.controllers;

import com.nexevent.nexevent.services.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("${nexevent.api-prefix}/dashboard")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin Dashboard", description = "API Thống kê & Realtime cho Ban tổ chức")
public class DashboardController {

    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    @Operation(
            summary = "Mở luồng Live Data (SSE) theo sự kiện",
            description = "Dành cho Frontend gọi để lắng nghe sự kiện Check-in và cập nhật các widget biểu đồ realtime của từng sự kiện"
    )
    @GetMapping(value = "/events/{eventId}/live-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDashboardStats(@PathVariable Long eventId) {
        return dashboardService.subscribe(eventId);
    }
}