package vn.edu.tvu.vinhlongtour.cancellation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.auth.security.CurrentAccount;
import vn.edu.tvu.vinhlongtour.booking.domain.CancellationRequestStatus;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingDetail;
import vn.edu.tvu.vinhlongtour.cancellation.CancellationDtos.CancellationInput;
import vn.edu.tvu.vinhlongtour.cancellation.CancellationDtos.CancellationRequestSummary;
import vn.edu.tvu.vinhlongtour.cancellation.CancellationDtos.DecisionRequest;
import vn.edu.tvu.vinhlongtour.cancellation.CancellationDtos.RefundRequest;
import vn.edu.tvu.vinhlongtour.cancellation.CancellationDtos.TransferRequest;
import vn.edu.tvu.vinhlongtour.common.PageResponse;

/**
 * API hủy tour (khách gửi yêu cầu, admin duyệt/từ chối), hoàn tiền và chuyển đợt (admin).
 */
@RestController
public class CancellationController {

    private final CancellationService service;

    public CancellationController(CancellationService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/bookings/{code}/cancellation-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDetail request(@PathVariable String code, @Valid @RequestBody CancellationInput r, Authentication auth) {
        return service.requestCancellation(code, r, CurrentAccount.idOrNull(auth));
    }

    /** Mặc định sắp yêu cầu cũ nhất lên đầu; lọc {@code status=PENDING} để xem các yêu cầu chờ duyệt. */
    @GetMapping("/api/v1/admin/cancellation-requests")
    public PageResponse<CancellationRequestSummary> list(@RequestParam(required = false) CancellationRequestStatus status,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return service.list(status, page, size);
    }

    @PostMapping("/api/v1/admin/cancellation-requests/{id}/approve")
    public BookingDetail approve(Authentication auth, @PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest r) {
        return service.approve(id, r == null ? null : r.note(), Long.valueOf(auth.getName()));
    }

    @PostMapping("/api/v1/admin/cancellation-requests/{id}/reject")
    public BookingDetail reject(Authentication auth, @PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest r) {
        return service.reject(id, r == null ? null : r.note(), Long.valueOf(auth.getName()));
    }

    @PostMapping("/api/v1/admin/bookings/{id}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDetail refund(Authentication auth, @PathVariable Long id, @Valid @RequestBody RefundRequest r) {
        return service.refund(id, r, Long.valueOf(auth.getName()));
    }

    @PostMapping("/api/v1/admin/bookings/{id}/transfer")
    public BookingDetail transfer(Authentication auth, @PathVariable Long id, @Valid @RequestBody TransferRequest r) {
        return service.transfer(id, r, Long.valueOf(auth.getName()));
    }
}
