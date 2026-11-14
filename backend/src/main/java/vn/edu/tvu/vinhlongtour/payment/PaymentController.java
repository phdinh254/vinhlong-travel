package vn.edu.tvu.vinhlongtour.payment;

import jakarta.servlet.http.HttpServletRequest;
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
import vn.edu.tvu.vinhlongtour.common.AppProperties;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.ManualPaymentRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.VnPayLinkRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.VnPayLinkResponse;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.VnPayReturnResponse;
import vn.edu.tvu.vinhlongtour.payment.PaymentService.CallbackOutcome;
import vn.edu.tvu.vinhlongtour.payment.PaymentService.CallbackResult;

import java.util.Map;

/**
 * API thanh toán: khách tạo link VNPay; trang kết quả và IPN nhận dữ liệu VNPay gửi về;
 * admin xác nhận chuyển khoản/tiền mặt.
 */
@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService service;
    private final boolean processOnReturn;

    public PaymentController(PaymentService service, AppProperties props) {
        this.service = service;
        this.processOnReturn = props.vnpay().processOnReturn();
    }

    @PostMapping("/api/v1/payments/vnpay")
    @ResponseStatus(HttpStatus.CREATED)
    public VnPayLinkResponse createLink(@Valid @RequestBody VnPayLinkRequest r, Authentication auth, HttpServletRequest request) {
        // VNPay chỉ nhận địa chỉ IPv4; khi chạy localhost địa chỉ thường là IPv6 (::1)
        String ip = request.getRemoteAddr().contains(":") ? "127.0.0.1" : request.getRemoteAddr();
        return service.createVnPayLink(r, CurrentAccount.idOrNull(auth), ip);
    }

    /**
     * Frontend gọi API này với nguyên chuỗi tham số VNPay gắn vào đường dẫn trang kết quả.
     * Khi bật {@code process-on-return} thì ghi nhận luôn kết quả (cần khi chạy localhost, VNPay không gọi IPN được).
     */
    @GetMapping("/api/v1/payments/vnpay/return")
    public VnPayReturnResponse vnpayReturn(@RequestParam Map<String, String> params) {
        CallbackOutcome outcome = processOnReturn ? service.processVnPayCallback(params) : null;
        boolean valid = outcome == null || outcome.result() != CallbackResult.INVALID_SIGNATURE;
        // Chỉ báo thành công khi hệ thống đã ghi nhận được (không sai số tiền, có giao dịch), không chỉ dựa vào mã VNPay
        boolean recorded = outcome == null || outcome.result() == CallbackResult.CONFIRMED || outcome.result() == CallbackResult.ALREADY_CONFIRMED;
        boolean success = valid && recorded && "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        String message = !valid ? "Dữ liệu thanh toán không hợp lệ"
                : !recorded ? "Không ghi nhận được giao dịch, vui lòng liên hệ để được hỗ trợ"
                : success ? "Thanh toán thành công"
                : "Giao dịch không thành công (mã " + params.get("vnp_ResponseCode") + ")";
        String reference = params.getOrDefault("vnp_TxnRef", "");
        return new VnPayReturnResponse(valid, success, outcome == null ? reference.split("-")[0] : outcome.bookingCode(),
                outcome == null ? null : outcome.amount(), params.get("vnp_TransactionNo"), params.get("vnp_ResponseCode"), message);
    }

    /** VNPay gọi thẳng vào đây (server tới server) để báo kết quả; khai báo URL này làm IPN URL trên cổng VNPay. */
    @GetMapping("/api/v1/payments/vnpay/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        try {
            return service.processVnPayCallback(params).result().toIpnResponse();
        } catch (RuntimeException e) {
            log.error("Lỗi khi xử lý IPN VNPay {}", params.get("vnp_TxnRef"), e);
            return Map.of("RspCode", "99", "Message", "Unknown error");
        }
    }

    @PostMapping("/api/v1/admin/bookings/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDetail recordManual(Authentication auth, @PathVariable Long id, @Valid @RequestBody ManualPaymentRequest r) {
        return service.recordManualPayment(id, r, Long.valueOf(auth.getName()));
    }
}
