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
import vn.edu.tvu.vinhlongtour.payment.PayOsGateway.CreatedLink;
import vn.edu.tvu.vinhlongtour.payment.PayOsGateway.PaymentInfo;
import vn.edu.tvu.vinhlongtour.payment.PayOsGateway.Transaction;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.ManualPaymentRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.PayOsLinkRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.PayOsLinkResponse;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.DepartureStatus;
import vn.edu.tvu.vinhlongtour.tour.repository.DepartureRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Nghiệp vụ thanh toán: tạo link payOS, ghi nhận kết quả payOS (webhook có chữ ký, hoặc hỏi lại payOS khi khách
 * quay về trang kết quả) và admin xác nhận chuyển khoản/tiền mặt. Mọi khoản tiền vào đều xử lý khi đã khóa dòng đơn,
 * nên webhook và trang kết quả cùng báo một giao dịch cũng không làm mất hay cộng trùng tiền.
 */
@Service
@Transactional
public class PaymentService {

    /** Kết quả xử lý một thông báo thanh toán từ payOS. */
    public enum CallbackResult {
        CONFIRMED,
        NOT_FOUND,
        ALREADY_CONFIRMED,
        INVALID_AMOUNT,
        INVALID_SIGNATURE,
        /** payOS chưa có kết quả cuối (khách chưa trả, hoặc đang xử lý). */
        STILL_PENDING
    }

    /** {@code paid}: hệ thống đã ghi nhận được tiền của lần thanh toán này. */
    public record CallbackOutcome(CallbackResult result, String bookingCode, Long amount, boolean paid, String status, String transactionNo) {}

    private static final DateTimeFormatter PAYOS_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final DepartureRepository departures;
    private final AccountRepository accounts;
    private final BookingService bookingService;
    private final PriceCalculator calculator;
    private final PayOsGateway gateway;
    private final Duration linkTimeout;
    private final Duration hold;
    private final Clock clock;

    public PaymentService(BookingRepository bookings, PaymentRepository payments, DepartureRepository departures,
                          AccountRepository accounts, BookingService bookingService, PriceCalculator calculator,
                          PayOsGateway gateway, AppProperties props, Clock clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.departures = departures;
        this.accounts = accounts;
        this.bookingService = bookingService;
        this.calculator = calculator;
        this.gateway = gateway;
        this.linkTimeout = props.payos().linkTimeout();
        this.hold = props.booking().hold();
        this.clock = clock;
    }

    /**
     * Tạo link payOS. Mỗi đơn chỉ có một link đang chờ trong thời gian link còn hiệu lực,
     * để khách không lỡ trả hai lần ở hai thẻ trình duyệt. payOS từ chối thì không lưu gì (giao dịch bị hủy).
     */
    public PayOsLinkResponse createPayOsLink(PayOsLinkRequest r, Long accountId) {
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
                    "Đơn đang có một giao dịch payOS chờ thanh toán; hãy hoàn tất giao dịch đó hoặc thử lại sau " + linkTimeout.toMinutes() + " phút");
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
        long orderCode = orderCode(payment.getId());
        payment.assignReference(String.valueOf(orderCode));
        CreatedLink link = gateway.createPaymentLink(orderCode, amount, description(booking.getCode()),
                clock.instant().plus(linkTimeout).getEpochSecond());
        return new PayOsLinkResponse(link.checkoutUrl(), link.qrCode(), amount, payment.getReference(), now.plus(linkTimeout));
    }

    /**
     * Webhook payOS (server tới server). Không tin dữ liệu nếu sai chữ ký hoặc sai số tiền; thanh toán đã xử lý rồi
     * thì bỏ qua (payOS có thể gửi lại). Khóa đơn trước rồi mới đọc trạng thái thanh toán.
     */
    public CallbackOutcome processWebhook(Map<String, Object> data, String signature) {
        if (!gateway.verifyData(data, signature)) {
            return new CallbackOutcome(CallbackResult.INVALID_SIGNATURE, null, null, false, null, null);
        }
        String reference = data.get("orderCode") == null ? null : String.valueOf(data.get("orderCode"));
        Long bookingId = reference == null ? null : payments.findBookingIdByReference(reference).orElse(null);
        if (bookingId == null) {
            return new CallbackOutcome(CallbackResult.NOT_FOUND, null, null, false, null, null);
        }
        Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow();
        Payment payment = payments.findByReference(reference).orElseThrow();
        boolean success = "00".equals(String.valueOf(data.get("code")));
        String transactionNo = data.get("reference") == null ? null : String.valueOf(data.get("reference"));
        if (!amountMatches(data.get("amount"), payment.getAmount())) {
            return new CallbackOutcome(CallbackResult.INVALID_AMOUNT, booking.getCode(), payment.getAmount(), false, null, transactionNo);
        }
        if (!payment.isPending()) {
            return new CallbackOutcome(CallbackResult.ALREADY_CONFIRMED, booking.getCode(), payment.getAmount(), payment.isSettled(), null, transactionNo);
        }
        String raw = raw(data);
        if (success) {
            LocalDateTime now = now();
            reviveIfExpired(booking, null, now);
            booking.confirmOnlinePayment(payment, transactionNo, str(data.get("counterAccountBankId")),
                    paidAt(str(data.get("transactionDateTime")), now), raw, now);
        } else {
            booking.failOnlinePayment(payment, raw);
        }
        return new CallbackOutcome(CallbackResult.CONFIRMED, booking.getCode(), payment.getAmount(), success, success ? "PAID" : null, transactionNo);
    }

    /**
     * Khách quay về trang kết quả: tham số payOS gắn vào đường dẫn không có chữ ký, nên hỏi lại payOS trạng thái thật
     * của link rồi mới ghi nhận. Nhờ vậy chạy trên máy cá nhân (payOS không gọi webhook vào localhost được) vẫn ghi nhận đúng.
     */
    public CallbackOutcome syncFromPayOs(String orderCodeText) {
        long orderCode;
        try {
            orderCode = Long.parseLong(orderCodeText == null ? "" : orderCodeText.trim());
        } catch (NumberFormatException e) {
            return new CallbackOutcome(CallbackResult.NOT_FOUND, null, null, false, null, null);
        }
        String reference = String.valueOf(orderCode);
        Long bookingId = payments.findBookingIdByReference(reference).orElse(null);
        if (bookingId == null) {
            return new CallbackOutcome(CallbackResult.NOT_FOUND, null, null, false, null, null);
        }
        Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow();
        Payment payment = payments.findByReference(reference).orElseThrow();
        PaymentInfo info = gateway.paymentInfo(orderCode);
        String transactionNo = info.transactions().isEmpty() ? null : info.transactions().getFirst().reference();
        if (info.amount() != payment.getAmount()) {
            return new CallbackOutcome(CallbackResult.INVALID_AMOUNT, booking.getCode(), payment.getAmount(), false, info.status(), transactionNo);
        }
        if (!payment.isPending()) {
            return new CallbackOutcome(CallbackResult.ALREADY_CONFIRMED, booking.getCode(), payment.getAmount(), payment.isSettled(),
                    info.status(), payment.getTransactionNo());
        }
        String raw = raw(Map.of("status", String.valueOf(info.status()), "amountPaid", info.amountPaid(), "transactions", info.transactions().toString()));
        if (info.isPaid() && info.amountPaid() >= payment.getAmount()) {
            LocalDateTime now = now();
            Transaction t = info.transactions().isEmpty() ? null : info.transactions().getFirst();
            reviveIfExpired(booking, null, now);
            booking.confirmOnlinePayment(payment, transactionNo, t == null ? null : t.counterAccountBankId(),
                    paidAt(t == null ? null : t.transactionDateTime(), now), raw, now);
            return new CallbackOutcome(CallbackResult.CONFIRMED, booking.getCode(), payment.getAmount(), true, info.status(), transactionNo);
        }
        if (info.isClosedWithoutPayment()) {
            booking.failOnlinePayment(payment, raw);
            return new CallbackOutcome(CallbackResult.CONFIRMED, booking.getCode(), payment.getAmount(), false, info.status(), null);
        }
        return new CallbackOutcome(CallbackResult.STILL_PENDING, booking.getCode(), payment.getAmount(), false, info.status(), null);
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

    /**
     * Mã đơn hàng gửi payOS phải là số nguyên và không trùng với mọi lần đã gửi trước đó (kể cả khi CSDL được tạo lại),
     * nên ghép giây hiện tại với 3 chữ số cuối của mã thanh toán: 13 chữ số, nhỏ hơn giới hạn số nguyên của JavaScript.
     */
    private long orderCode(long paymentId) {
        return (clock.instant().getEpochSecond() % 10_000_000_000L) * 1000 + paymentId % 1000;
    }

    /** Nội dung chuyển khoản tối đa 9 ký tự: "VL" + 7 ký tự cuối của mã đơn, đủ để đối chiếu sao kê. */
    private static String description(String bookingCode) {
        return "VL" + bookingCode.substring(Math.max(0, bookingCode.length() - 7));
    }

    private static boolean amountMatches(Object amount, long expected) {
        try {
            return amount != null && Long.parseLong(String.valueOf(amount)) == expected;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static LocalDateTime paidAt(String transactionDateTime, LocalDateTime fallback) {
        try {
            return transactionDateTime == null || transactionDateTime.isBlank() ? fallback : LocalDateTime.parse(transactionDateTime, PAYOS_TIME);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    /** Lưu lại dữ liệu payOS gửi về để tra soát khi có khiếu nại. */
    private static String raw(Map<String, ?> data) {
        String text = new TreeMap<>(data).toString();
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
