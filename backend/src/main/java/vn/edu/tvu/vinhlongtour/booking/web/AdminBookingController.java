package vn.edu.tvu.vinhlongtour.booking.web;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.booking.domain.BookingStatus;
import vn.edu.tvu.vinhlongtour.booking.service.BookingService;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingDetail;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingSummary;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.StatusChangeRequest;
import vn.edu.tvu.vinhlongtour.common.PageResponse;

/**
 * API quản trị đơn đặt tour (chỉ ADMIN): tìm/lọc, xem chi tiết, đổi trạng thái trực tiếp.
 */
@RestController
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingController {

    private final BookingService service;

    public AdminBookingController(BookingService service) {
        this.service = service;
    }

    /**
     * {@code q} tìm trong mã đơn, tên và SĐT người liên hệ.
     * {@code needsRefund=true}: chỉ các đơn đã thu thừa tiền, cần hoàn cho khách.
     */
    @GetMapping
    public PageResponse<BookingSummary> search(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) BookingStatus status,
                                               @RequestParam(required = false) Long tourId,
                                               @RequestParam(required = false) Long departureId,
                                               @RequestParam(defaultValue = "false") boolean needsRefund,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.search(q, status, tourId, departureId, needsRefund, page, size);
    }

    @GetMapping("/{id}")
    public BookingDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    public BookingDetail changeStatus(Authentication auth, @PathVariable Long id, @Valid @RequestBody StatusChangeRequest r) {
        return service.changeStatus(id, r.status(), r.reason(), Long.valueOf(auth.getName()));
    }
}
