package vn.edu.tvu.vinhlongtour.booking.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.auth.repository.AccountRepository;
import vn.edu.tvu.vinhlongtour.booking.domain.Booking;
import vn.edu.tvu.vinhlongtour.booking.domain.BookingStatus;
import vn.edu.tvu.vinhlongtour.booking.repository.BookingRepository;
import vn.edu.tvu.vinhlongtour.booking.service.PriceCalculator.PassengerLine;
import vn.edu.tvu.vinhlongtour.booking.service.PriceCalculator.Quote;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingDetail;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingRequest;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingSummary;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.PassengerQuote;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.QuoteRequest;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.QuoteResponse;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.SurchargeLine;
import vn.edu.tvu.vinhlongtour.catalog.repository.CancellationPolicyRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.PassengerTypeRepository;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.AppProperties;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.DepartureStatus;
import vn.edu.tvu.vinhlongtour.tour.repository.DepartureRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Nghiệp vụ đặt tour: báo giá, tạo đơn có giữ chỗ (khóa dòng đợt để không bán vượt chỗ),
 * tra cứu đơn, đơn của tôi, admin xem/lọc/đổi trạng thái, và trả chỗ cho đơn quá hạn giữ chỗ.
 */
@Service
@Transactional
public class BookingService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bỏ 0/O, 1/I dễ nhầm
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookings;
    private final DepartureRepository departures;
    private final PassengerTypeRepository passengerTypes;
    private final CancellationPolicyRepository policies;
    private final AccountRepository accounts;
    private final PriceCalculator calculator;
    private final Duration hold;
    private final Clock clock;

    public BookingService(BookingRepository bookings, DepartureRepository departures, PassengerTypeRepository passengerTypes,
                          CancellationPolicyRepository policies, AccountRepository accounts, PriceCalculator calculator,
                          AppProperties props, Clock clock) {
        this.bookings = bookings;
        this.departures = departures;
        this.passengerTypes = passengerTypes;
        this.policies = policies;
        this.accounts = accounts;
        this.calculator = calculator;
        this.hold = props.booking().hold();
        this.clock = clock;
    }

    // ---------- Khách ----------

    /** Báo giá, không giữ chỗ. */
    @Transactional(readOnly = true)
    public QuoteResponse quote(QuoteRequest r) {
        Departure departure = departures.findById(r.departureId()).filter(d -> d.getTour().isOnSale())
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Đợt khởi hành không tồn tại"));
        return toResponse(calculator.quote(departure, r.passengers(), r.surcharges(), passengerTypes.findAll()));
    }

    /**
     * Đặt tour. Khóa dòng đợt khởi hành trước khi kiểm tra và trừ chỗ, nên hai người tranh những chỗ cuối cùng
     * thì người đến sau phải chờ, rồi nhận lỗi hết chỗ.
     *
     * @param accountId tài khoản đang đăng nhập; null nếu khách vãng lai
     */
    public BookingDetail create(BookingRequest r, Long accountId) {
        LocalDateTime now = now();
        Departure departure = departures.findByIdForUpdate(r.departureId())
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Đợt khởi hành không tồn tại"));
        ensureOpenForBooking(departure, now);
        Quote q = calculator.quote(departure, r.passengers(), r.surcharges(), passengerTypes.findAll());
        departure.reserveSeats(q.seats());

        Account account = accountId == null ? null : accounts.getReferenceById(accountId);
        Booking booking = new Booking(newCode(now), departure, account,
                new Booking.Contact(r.contactName(), r.contactPhone(), blankToNull(r.contactEmail()), r.note()),
                q.passengers().stream().map(PassengerLine::details).toList(), q.surcharges(),
                policies.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(now.toLocalDate()).orElse(null),
                now, now.plus(hold));
        return detail(bookings.save(booking));
    }

    /** Khách vãng lai tra cứu bằng mã đơn + số điện thoại; sai một trong hai đều báo chung "không tìm thấy". */
    @Transactional(readOnly = true)
    public BookingDetail lookup(String code, String phone) {
        return bookings.findByCode(code.trim().toUpperCase()).filter(b -> b.matchesPhone(phone))
                .map(this::detail)
                .orElseThrow(BookingService::bookingNotFound);
    }

    @Transactional(readOnly = true)
    public List<BookingSummary> listMine(Long accountId) {
        return bookings.findByAccountIdOrderByCreatedAtDesc(accountId).stream().map(BookingSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public BookingDetail getMine(Long accountId, String code) {
        return bookings.findByCode(code.trim().toUpperCase()).filter(b -> b.belongsTo(accountId))
                .map(this::detail)
                .orElseThrow(BookingService::bookingNotFound);
    }

    // ---------- Admin ----------

    @Transactional(readOnly = true)
    public PageResponse<BookingSummary> search(String q, BookingStatus status, Long tourId, Long departureId, boolean needsRefund,
                                               int page, int size) {
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                where.add(cb.or(cb.like(cb.lower(root.get("code")), like), cb.like(cb.lower(root.get("contactName")), like),
                        cb.like(root.get("contactPhone"), like)));
            }
            if (status != null) {
                where.add(cb.equal(root.get("status"), status));
            }
            if (departureId != null) {
                where.add(cb.equal(root.get("departure").get("id"), departureId));
            }
            if (tourId != null) {
                where.add(cb.equal(root.get("departure").get("tour").get("id"), tourId));
            }
            if (needsRefund) {
                // Cùng công thức với Booking.refundDue(): đã thu nhiều hơn số được giữ lại
                Expression<Long> keep = cb.<Long>selectCase()
                        .when(root.get("status").in(BookingStatus.PENDING_PAYMENT, BookingStatus.DEPOSITED, BookingStatus.PAID,
                                BookingStatus.COMPLETED), root.<Long>get("totalAmount"))
                        .otherwise(root.<Long>get("cancellationFee"));
                where.add(cb.gt(root.<Long>get("paidAmount"), keep));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(bookings.findAll(spec, pageable).map(BookingSummary::from));
    }

    @Transactional(readOnly = true)
    public BookingDetail get(Long id) {
        return detail(bookings.findById(id).orElseThrow(BookingService::bookingNotFound));
    }

    /**
     * Admin đổi trạng thái trực tiếp: COMPLETED sau chuyến đi (đơn đã trả đủ), hoặc CANCELLED cho đơn chưa thu tiền.
     * Đơn đã thu tiền phải hủy qua quy trình hủy/hoàn tiền (giai đoạn 6).
     */
    public BookingDetail changeStatus(Long id, BookingStatus target, String reason, Long adminId) {
        Booking booking = bookings.findByIdForUpdate(id).orElseThrow(BookingService::bookingNotFound);
        Account admin = accounts.getReferenceById(adminId);
        switch (target) {
            case COMPLETED -> booking.moveTo(BookingStatus.COMPLETED, admin, reason, now());
            case CANCELLED -> {
                if (booking.getPaidAmount() > 0) {
                    throw ApiException.conflict("BOOKING_HAS_PAYMENTS", "Đơn đã thu tiền, hãy hủy qua yêu cầu hủy để tính phí và hoàn tiền");
                }
                releaseSeatsAndMove(booking, BookingStatus.CANCELLED, admin, reason);
            }
            default -> throw ApiException.badRequest("UNSUPPORTED_STATUS", "Admin chỉ đổi trực tiếp sang COMPLETED hoặc CANCELLED");
        }
        return detail(booking);
    }

    // ---------- Hệ thống ----------

    /** Mã các đơn đã quá hạn giữ chỗ tại thời điểm {@code now}. */
    @Transactional(readOnly = true)
    public List<Long> overdueHoldIds(LocalDateTime now) {
        return bookings.findOverdueHoldIds(BookingStatus.PENDING_PAYMENT, now);
    }

    /**
     * Trả chỗ cho một đơn quá hạn giữ chỗ. Khóa đơn trước rồi kiểm tra lại: nếu khách vừa thanh toán
     * ngay trước đó thì đơn không còn quá hạn và được giữ nguyên.
     *
     * @return true nếu đơn đã bị chuyển sang EXPIRED
     */
    public boolean expireIfOverdue(Long id, LocalDateTime now) {
        Booking booking = bookings.findByIdForUpdate(id).orElse(null);
        if (booking == null || !booking.isHoldExpired(now)) {
            return false;
        }
        releaseSeatsAndMove(booking, BookingStatus.EXPIRED, null, "Quá hạn giữ chỗ mà chưa thanh toán");
        return true;
    }

    // ---------- Hàm phụ ----------

    /** Khóa đợt khởi hành (sau khi đã khóa đơn) rồi trả chỗ và đổi trạng thái. */
    private void releaseSeatsAndMove(Booking booking, BookingStatus target, Account actor, String reason) {
        boolean heldSeats = booking.getStatus().holdsSeats();
        booking.moveTo(target, actor, reason, now());
        if (heldSeats) {
            departures.findByIdForUpdate(booking.getDeparture().getId()).orElseThrow().releaseSeats(booking.getSeats());
        }
    }

    private static void ensureOpenForBooking(Departure d, LocalDateTime now) {
        if (!d.getTour().isOnSale() || d.getStatus() != DepartureStatus.OPEN) {
            throw ApiException.conflict("DEPARTURE_NOT_OPEN", "Đợt khởi hành không còn nhận khách");
        }
        if (!now.isBefore(d.getBookingDeadline())) {
            throw ApiException.conflict("BOOKING_DEADLINE_PASSED", "Đã quá hạn nhận khách cho đợt này");
        }
    }

    private String newCode(LocalDateTime now) {
        String prefix = "VL" + now.format(CODE_DATE);
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(prefix);
            for (int i = 0; i < 5; i++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            if (!bookings.existsByCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("Không sinh được mã đơn sau 10 lần thử");
    }

    /** Chi tiết đơn kèm số tiền tối thiểu cần trả tiếp (tiền cọc nếu chưa trả gì, còn lại nếu đã trả một phần). */
    public BookingDetail detail(Booking b) {
        long next = b.getPaidAmount() == 0 ? calculator.deposit(b.getTotalAmount()) : b.remainingAmount();
        return BookingDetail.from(b, next);
    }

    private static QuoteResponse toResponse(Quote q) {
        return new QuoteResponse(
                q.passengers().stream().map(l -> new PassengerQuote(l.details().fullName(), l.age(), l.details().passengerType().getId(),
                        l.details().passengerType().getName(), l.details().price(),
                        l.details().pickupPoint() == null ? null : l.details().pickupPoint().getName(), l.details().pickupFee())).toList(),
                q.surcharges().stream().map(s -> new SurchargeLine(s.getSurcharge().getId(), s.getName(), s.getMethod(),
                        s.getQuantity(), s.getUnitPrice(), s.getAmount())).toList(),
                q.ticketTotal(), q.pickupTotal(), q.surchargeTotal(), q.total(), q.seats(), q.deposit());
    }

    private static ApiException bookingNotFound() {
        return ApiException.notFound("BOOKING_NOT_FOUND", "Không tìm thấy đơn, hoặc số điện thoại không khớp");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
