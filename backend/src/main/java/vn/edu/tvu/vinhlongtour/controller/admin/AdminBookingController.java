package vn.edu.tvu.vinhlongtour.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.tvu.vinhlongtour.dto.AdminDtos.*;
import vn.edu.tvu.vinhlongtour.dto.BookingDtos.*;
import vn.edu.tvu.vinhlongtour.service.BookingService;
import vn.edu.tvu.vinhlongtour.service.CancellationService;
import vn.edu.tvu.vinhlongtour.service.PaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBookingController {
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    @GetMapping("/bookings")
    public Page<BookingSummary> list(@RequestParam(required = false) String q, @RequestParam(required = false) String trangThai,
                                     @RequestParam(required = false) Integer tourId, @RequestParam(required = false) Integer dotId,
                                     @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return bookingService.adminSearch(q, trangThai, tourId, dotId, page, size);
    }

    @GetMapping("/bookings/{id}")
    public BookingDetail get(@PathVariable Integer id) {
        return bookingService.adminDetail(id);
    }

    @PutMapping("/bookings/{id}/trang-thai")
    public BookingDetail updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdateStatusRequest r) {
        return bookingService.adminUpdateStatus(id, r.trangThai(), r.lyDo());
    }

    @PostMapping("/bookings/{id}/thanh-toan")
    public BookingDetail manualPayment(@PathVariable Integer id, @Valid @RequestBody ManualPaymentRequest r) {
        return paymentService.confirmManual(id, r);
    }

    @PostMapping("/bookings/{id}/hoan-tien")
    public BookingDetail refund(@PathVariable Integer id, @Valid @RequestBody RefundRequest r) {
        return cancellationService.refund(id, r);
    }

    @PostMapping("/bookings/{id}/chuyen-dot")
    public BookingDetail transfer(@PathVariable Integer id, @Valid @RequestBody TransferRequest r) {
        return cancellationService.transfer(id, r);
    }

    @GetMapping("/yeu-cau-huy")
    public List<CancelDto> cancelRequests() {
        return cancellationService.listAll();
    }

    @PutMapping("/yeu-cau-huy/{id}")
    public BookingDetail processCancel(@PathVariable Integer id, @Valid @RequestBody ProcessCancelRequest r) {
        return cancellationService.process(id, r);
    }
}
