package vn.edu.tvu.vinhlongtour.payment;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingDetail;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.ManualPaymentRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.PayOsLinkRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.PayOsLinkResponse;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.PayOsReturnResponse;
import vn.edu.tvu.vinhlongtour.payment.PaymentService.CallbackOutcome;

import java.util.Map;

/**
 * API thanh toán: khách tạo link payOS; trang kết quả và webhook ghi nhận kết quả payOS;
 * admin xác nhận chuyển khoản/tiền mặt.
 */
@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/payments/payos")
    @ResponseStatus(HttpStatus.CREATED)
    public PayOsLinkResponse createLink(@Valid @RequestBody PayOsLinkRequest r, Authentication auth) {
        return service.createPayOsLink(r, CurrentAccount.idOrNull(auth));
    }

    /**
     * Frontend gọi API này với {@code orderCode} payOS gắn vào đường dẫn trang kết quả. Backend hỏi lại payOS
     * trạng thái thật rồi mới ghi nhận, nên không tin tham số trên đường dẫn.
     */
    @GetMapping("/api/v1/payments/payos/return")
    public PayOsReturnResponse payOsReturn(@RequestParam String orderCode) {
        CallbackOutcome o = service.syncFromPayOs(orderCode);
        String message = switch (o.result()) {
            case NOT_FOUND -> "Không tìm thấy giao dịch";
            case INVALID_AMOUNT, INVALID_SIGNATURE -> "Không ghi nhận được giao dịch, vui lòng liên hệ để được hỗ trợ";
            case STILL_PENDING -> "Giao dịch đang chờ xử lý";
            case CONFIRMED, ALREADY_CONFIRMED -> o.paid() ? "Thanh toán thành công" : "Giao dịch không thành công";
        };
        return new PayOsReturnResponse(o.paid(), o.status(), o.bookingCode(), o.amount(), o.transactionNo(), message);
    }

    /**
     * payOS gọi thẳng vào đây (server tới server) khi có tiền vào; khai báo URL này làm webhook trên my.payos.vn.
     * Luôn trả 200 (kể cả dữ liệu không hợp lệ) vì payOS gửi một bản thử khi xác nhận URL và chỉ cần biết đã nhận.
     */
    @PostMapping("/api/v1/payments/payos/webhook")
    @SuppressWarnings("unchecked")
    public Map<String, Object> payOsWebhook(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> data = body.get("data") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
            CallbackOutcome o = service.processWebhook(data, body.get("signature") == null ? null : String.valueOf(body.get("signature")));
            return Map.of("success", true, "result", o.result().name());
        } catch (RuntimeException e) {
            log.error("Lỗi khi xử lý webhook payOS {}", body, e);
            return Map.of("success", false);
        }
    }

    @PostMapping("/api/v1/admin/bookings/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDetail recordManual(Authentication auth, @PathVariable Long id, @Valid @RequestBody ManualPaymentRequest r) {
        return service.recordManualPayment(id, r, Long.valueOf(auth.getName()));
    }
}
