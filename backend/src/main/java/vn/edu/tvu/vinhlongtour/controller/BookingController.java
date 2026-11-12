package vn.edu.tvu.vinhlongtour.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.tvu.vinhlongtour.dto.BookingDtos.*;
import vn.edu.tvu.vinhlongtour.service.BookingService;
import vn.edu.tvu.vinhlongtour.service.CancellationService;

/** Đặt tour: dùng chung cho khách hàng đăng nhập và khách vãng lai */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final CancellationService cancellationService;

    /** Báo giá trước khi đặt (không giữ chỗ) */
    @PostMapping("/bao-gia")
    public Quote quote(@Valid @RequestBody CreateBookingRequest r) {
        return bookingService.quote(r);
    }

    @PostMapping
    public BookingDetail create(@Valid @RequestBody CreateBookingRequest r) {
        return bookingService.create(r);
    }

    /** Tra cứu đơn: khách vãng lai truyền ?sdt= ; khách đăng nhập/admin không cần */
    @GetMapping("/{maDon}")
    public BookingDetail get(@PathVariable String maDon, @RequestParam(required = false) String sdt) {
        return bookingService.getForCustomer(maDon, sdt);
    }

    @PostMapping("/{maDon}/yeu-cau-huy")
    public BookingDetail cancel(@PathVariable String maDon, @Valid @RequestBody CancelRequestInput r) {
        return cancellationService.requestCancel(maDon, r);
    }
}
