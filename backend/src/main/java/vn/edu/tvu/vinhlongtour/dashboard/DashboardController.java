package vn.edu.tvu.vinhlongtour.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.dashboard.DashboardService.Dashboard;

import java.time.LocalDate;

/**
 * API số liệu tổng quan cho trang quản trị. Không truyền ngày thì tính từ đầu tháng tới hôm nay.
 */
@RestController
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/dashboard")
    public Dashboard dashboard(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        return service.get(from, to);
    }
}
