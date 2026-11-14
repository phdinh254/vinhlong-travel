package vn.edu.tvu.vinhlongtour.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.auth.repository.AccountRepository;
import vn.edu.tvu.vinhlongtour.booking.domain.Booking;
import vn.edu.tvu.vinhlongtour.booking.domain.BookingStatus;
import vn.edu.tvu.vinhlongtour.booking.domain.Payment;
import vn.edu.tvu.vinhlongtour.booking.repository.BookingRepository;
import vn.edu.tvu.vinhlongtour.booking.repository.PaymentRepository;
import vn.edu.tvu.vinhlongtour.booking.service.BookingService;
import vn.edu.tvu.vinhlongtour.booking.service.PriceCalculator;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingDetail;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.AppProperties;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.ManualPaymentRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.VnPayLinkRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.VnPayLinkResponse;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.DepartureStatus;
import vn.edu.tvu.vinhlongtour.tour.repository.DepartureRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Nghiệp vụ thanh toán: tạo link VNPay, xử lý kết quả VNPay gửi về (IPN/trang kết quả) và admin xác nhận
 * chuyển khoản/tiền mặt. Mọi khoản tiền vào đều xử lý khi đã khóa dòng đơn, nên VNPay gửi hai thông báo
 * cùng lúc cũng không làm mất hay cộng trùng tiền.
 */
@Service
@Transactional
public class PaymentService {

    /** Kết quả xử lý dữ liệu VNPay gửi về, kèm mã trả lời IPN theo tài liệu VNPay. */
    public enum CallbackResult {
        CONFIRMED("00", "Confirm Success"),
        NOT_FOUND("01", "Order not found"),
        ALREADY_CONFIRMED("02", "Order already confirmed"),
        INVALID_AMOUNT("04", "Invalid amount"),
        INVALID_SIGNATURE("97", "Invalid Checksum");

        private final String rspCode;
        private final String message;

        CallbackResult(String rspCode, String message) {
            this.rspCode = rspCode;
            this.message = message;
        }

        public Map<String, String> toIpnResponse() {
            return Map.of("RspCode", rspCode, "Message", message);
        }
    }

    /** {@code success}: VNPay báo giao dịch thành công (mã 00); chỉ có nghĩa khi chữ ký hợp lệ. */
    public record CallbackOutcome(CallbackResult result, String bookingCode, Long amount, boolean success) {}

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final DepartureRepository departures;
    private final AccountRepository accounts;
    private final BookingService bookingService;
    private final PriceCalculator calculator;
    private final VnPayGateway gateway;
    private final Duration linkTimeout;
    private final Duration hold;
    private final Clock clock;

    public PaymentService(BookingRepository bookings, PaymentRepository payments, DepartureRepository departures,
                          AccountRepository accounts, BookingService bookingService, PriceCalculator calculator,
                          VnPayGateway gateway, AppProperties props, Clock clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.departures = departures;
        this.accounts = accounts;
        this.bookingService = bookingService;
        this.calculator = calculator;
        this.gateway = gateway;
        this.linkTimeout = props.vnpay().linkTimeout();
        this.hold = props.booking().hold();
        this.clock = clock;
    }

    /**
     * Tạo link VNPay. Mỗi đơn chỉ có một link đang chờ trong thời gian link còn hiệu lực,
     * để khách không lỡ trả hai lần ở hai thẻ trình duyệt.
     */
    public VnPayLinkResponse createVnPayLink(VnPayLinkRequest r, Long accountId, String ipAddress) {
        LocalDateTime now = now();
        Long bookingId = bookings.findByCode(r.bookingCode().trim().toUpperCase())
                .filter(b -> b.belongsTo(accountId) || b.matchesPhone(r.phone()))
                .map(Booking::getId)
                .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND", "Không tìm thấy đơn, hoặc số điện thoại không khớp"));
        Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow();
        if (!booking.isPayable(now)) {
            throw ApiException.conflict("BOOKING_NOT_PAYABLE", "Đơn không ở trạng thái chờ thanh toán hoặc đã hết hạn giữ chỗ");
        }
        if (booking.hasPendingOnlinePaymentSince(now.minus(linkTimeout))) {
            throw ApiException.conflict("PAYMENT_IN_PROGRESS",
                    "Đơn đang có một giao dịch VNPay chờ thanh toán; hãy hoàn tất giao dịch đó hoặc thử lại sau " + linkTimeout.toMinutes() + " phút");
        }
        long amount = switch (r.type()) {
            case DEPOSIT -> {
                if (booking.getPaidAmount() > 0) {
                    throw ApiException.badRequest("DEPOSIT_ALREADY_PAID", "Đơn đã đặt cọc, hãy thanh toán phần còn lại");
                }
                yield calculator.deposit(booking.getTotalAmount());
            }
            case FULL -> booking.remainingAmount();
        };
        Payment payment = payments.save(booking.startOnlinePayment(amount, now));
        payment.assignReference(booking.getCode() + "-" + payment.getId());
        String url = gateway.paymentUrl(payment.getReference(), amount, "Thanh toan don " + booking.getCode(), ipAddress, now);
        return new VnPayLinkResponse(url, amount, payment.getReference(), now.plus(linkTimeout));
    }

    /**
     * Xử lý kết quả VNPay gửi về. Không tin dữ liệu nếu sai chữ ký hoặc sai số tiền; thanh toán đã xử lý rồi
     * thì bỏ qua (VNPay có thể gửi IPN nhiều lần). Khóa đơn trước rồi mới đọc trạng thái thanh toán.
     */
    public CallbackOutcome processVnPayCallback(Map<String, String> params) {
        if (!gateway.verify(params)) {
            return new CallbackOutcome(CallbackResult.INVALID_SIGNATURE, null, null, false);
        }
        String reference = params.get("vnp_TxnRef");
        Long bookingId = reference == null ? null : payments.findBookingIdByReference(reference).orElse(null);
        if (bookingId == null) {
            return new CallbackOutcome(CallbackResult.NOT_FOUND, null, null, false);
        }
        Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow();
        Payment payment = payments.findByReference(reference).orElseThrow();
        boolean success = "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        if (!amountMatches(params.get("vnp_Amount"), payment.getAmount())) {
            return new CallbackOutcome(CallbackResult.INVALID_AMOUNT, booking.getCode(), payment.getAmount(), false);
        }
        if (!payment.isPending()) {
            return new CallbackOutcome(CallbackResult.ALREADY_CONFIRMED, booking.getCode(), payment.getAmount(), success);
        }
        String raw = raw(params);
        if (success) {
            LocalDateTime now = now();
            reviveIfExpired(booking, null, now);
            booking.confirmOnlinePayment(payment, params.get("vnp_TransactionNo"), params.get("vnp_BankCode"),
                    paidAt(params.get("vnp_PayDate"), now), raw, now);
        } else {
            booking.failOnlinePayment(payment, raw);
        }
        return new CallbackOutcome(CallbackResult.CONFIRMED, booking.getCode(), payment.getAmount(), success);
    }

    /** Admin xác nhận đã nhận chuyển khoản/tiền mặt kèm chứng từ. */
    public BookingDetail recordManualPayment(Long bookingId, ManualPaymentRequest r, Long adminId) {
        LocalDateTime now = now();
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND", "Không tìm thấy đơn"));
        Account admin = accounts.getReferenceById(adminId);
        BookingStatus status = booking.getStatus();
        if (status != BookingStatus.PENDING_PAYMENT && status != BookingStatus.DEPOSITED && status != BookingStatus.EXPIRED) {
            throw ApiException.conflict("BOOKING_NOT_PAYABLE", "Đơn ở trạng thái " + status + " không nhận thêm tiền");
        }
        reviveIfExpired(booking, admin, now);
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw ApiException.conflict("NO_SEATS_TO_REVIVE", "Đơn đã hết hạn và đợt không còn đủ chỗ; không nhận tiền cho đơn này");
        }
        booking.recordManualPayment(r.amount(), r.method(), blankToNull(r.transactionNo()), blankToNull(r.proofUrl()), r.note(), admin, now);
        return bookingService.detail(booking);
    }

    /**
     * Đơn đã hết hạn giữ chỗ mà nhận được tiền: nếu đợt chưa khởi hành, chưa hủy và còn đủ chỗ thì giữ lại chỗ
     * và khôi phục đơn; nếu không thì đơn vẫn hết hạn và số tiền nhận được hiện là "cần hoàn".
     */
    private void reviveIfExpired(Booking booking, Account actor, LocalDateTime now) {
        if (booking.getStatus() != BookingStatus.EXPIRED) {
            return;
        }
        Departure d = departures.findByIdForUpdate(booking.getDeparture().getId()).orElseThrow();
        if (d.getStatus() != DepartureStatus.CANCELLED && d.getDepartureAt().isAfter(now) && d.seatsLeft() >= booking.getSeats()) {
            d.reserveSeats(booking.getSeats());
            booking.revive(actor, now, now.plus(hold));
        }
    }

    /** vnp_Amount là số đồng × 100; phải khớp đúng số tiền của lần thanh toán. */
    private static boolean amountMatches(String vnpAmount, long expected) {
        try {
            return vnpAmount != null && Long.parseLong(vnpAmount) == expected * 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static LocalDateTime paidAt(String vnpPayDate, LocalDateTime fallback) {
        try {
            return vnpPayDate == null ? fallback : LocalDateTime.parse(vnpPayDate, VnPayGateway.VNP_TIME);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    /** Lưu lại dữ liệu VNPay gửi về (bỏ chữ ký) để tra soát khi có khiếu nại. */
    private static String raw(Map<String, String> params) {
        Map<String, String> copy = new TreeMap<>(params);
        copy.remove("vnp_SecureHash");
        String text = copy.toString();
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
