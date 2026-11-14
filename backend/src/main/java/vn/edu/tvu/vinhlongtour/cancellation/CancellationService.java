package vn.edu.tvu.vinhlongtour.cancellation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.auth.repository.AccountRepository;
import vn.edu.tvu.vinhlongtour.booking.domain.Booking;
import vn.edu.tvu.vinhlongtour.booking.domain.CancellationRequest;
import vn.edu.tvu.vinhlongtour.booking.domain.CancellationRequestStatus;
import vn.edu.tvu.vinhlongtour.booking.repository.BookingRepository;
import vn.edu.tvu.vinhlongtour.booking.repository.CancellationRequestRepository;
import vn.edu.tvu.vinhlongtour.booking.service.BookingService;
import vn.edu.tvu.vinhlongtour.booking.service.PriceCalculator;
import vn.edu.tvu.vinhlongtour.booking.service.PriceCalculator.PassengerLine;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingDetail;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.PassengerInput;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.SurchargeSelection;
import vn.edu.tvu.vinhlongtour.cancellation.CancellationDtos.CancellationInput;
import vn.edu.tvu.vinhlongtour.cancellation.CancellationDtos.CancellationRequestSummary;
import vn.edu.tvu.vinhlongtour.cancellation.CancellationDtos.RefundRequest;
import vn.edu.tvu.vinhlongtour.cancellation.CancellationDtos.TransferRequest;
import vn.edu.tvu.vinhlongtour.catalog.repository.PassengerTypeRepository;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.DepartureStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ hủy tour, hoàn tiền và chuyển đợt. Mọi thao tác đều khóa dòng đơn trước, rồi mới khóa
 * đợt khởi hành (chuyển đợt khóa hai đợt theo thứ tự mã tăng dần) để hai giao dịch không chờ nhau mãi.
 */
@Service
@Transactional
public class CancellationService {

    private final BookingRepository bookings;
    private final CancellationRequestRepository requests;
    private final EntityManager entityManager;
    private final PassengerTypeRepository passengerTypes;
    private final AccountRepository accounts;
    private final BookingService bookingService;
    private final PriceCalculator calculator;
    private final Clock clock;

    public CancellationService(BookingRepository bookings, CancellationRequestRepository requests, EntityManager entityManager,
                               PassengerTypeRepository passengerTypes, AccountRepository accounts, BookingService bookingService,
                               PriceCalculator calculator, Clock clock) {
        this.bookings = bookings;
        this.requests = requests;
        this.entityManager = entityManager;
        this.passengerTypes = passengerTypes;
        this.accounts = accounts;
        this.bookingService = bookingService;
        this.calculator = calculator;
        this.clock = clock;
    }

    /**
     * Khách (chủ tài khoản, hoặc khách vãng lai kèm SĐT) gửi yêu cầu hủy. Đơn chưa thu đồng nào thì không có tiền
     * để tranh chấp nên hệ thống duyệt ngay.
     */
    public BookingDetail requestCancellation(String bookingCode, CancellationInput r, Long accountId) {
        LocalDateTime now = now();
        Long bookingId = bookings.findByCode(bookingCode.trim().toUpperCase())
                .filter(b -> b.belongsTo(accountId) || b.matchesPhone(r.phone()))
                .map(Booking::getId)
                .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND", "Không tìm thấy đơn, hoặc số điện thoại không khớp"));
        Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow();
        Account actor = accountId == null ? null : accounts.getReferenceById(accountId);
        CancellationRequest request = booking.requestCancellation(r.scope(), r.passengerIds(), r.reason(), actor, now);
        if (booking.getPaidAmount() == 0) {
            releaseSeats(booking, booking.approveCancellation(request, null, "Tự duyệt: đơn chưa thanh toán", now));
        }
        return detail(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<CancellationRequestSummary> list(CancellationRequestStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.ASC, "requestedAt"));
        var result = status == null ? requests.findAll(pageable) : requests.findByStatus(status, pageable);
        return PageResponse.of(result.map(CancellationRequestSummary::from));
    }

    public BookingDetail approve(Long requestId, String note, Long adminId) {
        Booking booking = lockBookingOf(requestId);
        int released = booking.approveCancellation(requestOf(booking, requestId), accounts.getReferenceById(adminId), note, now());
        releaseSeats(booking, released);
        return detail(booking);
    }

    public BookingDetail reject(Long requestId, String note, Long adminId) {
        Booking booking = lockBookingOf(requestId);
        booking.rejectCancellation(requestOf(booking, requestId), accounts.getReferenceById(adminId), note, now());
        return detail(booking);
    }

    public BookingDetail refund(Long bookingId, RefundRequest r, Long adminId) {
        Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow(CancellationService::bookingNotFound);
        CancellationRequest request = r.cancellationRequestId() == null ? null : requestOf(booking, r.cancellationRequestId());
        booking.refund(r.amount(), r.method(), blankToNull(r.transactionNo()), blankToNull(r.proofUrl()), r.note(), request,
                accounts.getReferenceById(adminId), now());
        return detail(booking);
    }

    /**
     * Chuyển đơn sang đợt khác của cùng tour: tính lại giá theo đợt mới (tuổi theo ngày đi mới, giữ điểm đón
     * nếu đợt mới cũng có), trả chỗ ở đợt cũ và giữ chỗ ở đợt mới.
     */
    public BookingDetail transfer(Long bookingId, TransferRequest r, Long adminId) {
        LocalDateTime now = now();
        Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow(CancellationService::bookingNotFound);

        // Khóa hai đợt theo thứ tự mã tăng dần: hai admin chuyển đơn ngược chiều nhau cùng lúc không chờ nhau mãi
        Long fromId = booking.getDeparture().getId();
        Map<Long, Departure> locked = new HashMap<>();
        List.of(fromId, r.departureId()).stream().distinct().sorted().forEach(id -> locked.put(id, lockDeparture(id)));
        Departure from = locked.get(fromId);
        Departure to = locked.get(r.departureId());
        booking.ensureTransferable(to);
        if (!to.getTour().isOnSale() || to.getStatus() != DepartureStatus.OPEN || !now.isBefore(to.getBookingDeadline())) {
            throw ApiException.conflict("DEPARTURE_NOT_OPEN", "Đợt mới không còn nhận khách");
        }

        List<PassengerInput> inputs = booking.activePassengers().stream()
                .map(p -> new PassengerInput(p.getFullName(), p.getBirthDate(), p.getGender(), p.getPhone(), p.getIdNumber(),
                        p.getPickupPoint() != null && to.pickupAt(p.getPickupPoint().getId()).isPresent() ? p.getPickupPoint().getId() : null))
                .toList();
        List<SurchargeSelection> selections = booking.getSurcharges().stream()
                .map(s -> new SurchargeSelection(s.getSurcharge().getId(), s.getQuantity())).toList();
        PriceCalculator.Quote quote = calculator.quote(to, inputs, selections, passengerTypes.findAll());

        from.releaseSeats(booking.getSeats());
        to.reserveSeats(quote.seats());
        booking.transferTo(to, quote.passengers().stream().map(PassengerLine::details).toList(), quote.surcharges(),
                accounts.getReferenceById(adminId), r.reason(), now);
        return detail(booking);
    }

    /** Ghi xuống DB trước để yêu cầu hủy, khoản hoàn mới có mã trong phản hồi. */
    private BookingDetail detail(Booking booking) {
        entityManager.flush();
        return bookingService.detail(booking);
    }

    private Booking lockBookingOf(Long requestId) {
        Long bookingId = requests.findBookingId(requestId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Không tìm thấy yêu cầu hủy"));
        return bookings.findByIdForUpdate(bookingId).orElseThrow();
    }

    private static CancellationRequest requestOf(Booking booking, Long requestId) {
        return booking.getCancellationRequests().stream().filter(x -> x.getId().equals(requestId)).findFirst()
                .orElseThrow(() -> ApiException.badRequest("REQUEST_NOT_OF_BOOKING", "Yêu cầu hủy không thuộc đơn này"));
    }

    /** Trả chỗ cho đợt khởi hành (sau khi đã khóa đơn). */
    private void releaseSeats(Booking booking, int seats) {
        if (seats > 0) {
            lockDeparture(booking.getDeparture().getId()).releaseSeats(seats);
        }
    }

    /**
     * Khóa dòng đợt khởi hành và đọc lại số chỗ mới nhất. Dùng refresh thay vì truy vấn khóa thường vì đợt có thể
     * đã được đọc (không khóa) trước đó trong cùng giao dịch; truy vấn khóa sẽ trả lại bản cũ đó mà không đọc lại.
     */
    private Departure lockDeparture(Long id) {
        Departure d = entityManager.find(Departure.class, id);
        if (d == null) {
            throw ApiException.notFound("NOT_FOUND", "Đợt khởi hành không tồn tại");
        }
        entityManager.refresh(d, LockModeType.PESSIMISTIC_WRITE);
        return d;
    }

    private static ApiException bookingNotFound() {
        return ApiException.notFound("BOOKING_NOT_FOUND", "Không tìm thấy đơn");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
