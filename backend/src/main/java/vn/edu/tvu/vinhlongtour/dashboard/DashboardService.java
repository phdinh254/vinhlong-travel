package vn.edu.tvu.vinhlongtour.dashboard;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.booking.domain.BookingStatus;
import vn.edu.tvu.vinhlongtour.booking.domain.CancellationRequestStatus;
import vn.edu.tvu.vinhlongtour.booking.domain.PaymentStatus;
import vn.edu.tvu.vinhlongtour.booking.repository.CancellationRequestRepository;
import vn.edu.tvu.vinhlongtour.booking.service.BookingService;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingSummary;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.content.domain.ContactStatus;
import vn.edu.tvu.vinhlongtour.content.repository.ContactMessageRepository;
import vn.edu.tvu.vinhlongtour.tour.domain.TourStatus;
import vn.edu.tvu.vinhlongtour.tour.repository.TourRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Số liệu tổng quan cho trang quản trị: tiền thu và tiền hoàn trong khoảng ngày, số đơn mới theo trạng thái,
 * và các việc admin cần làm ngay (đơn thu thừa cần hoàn tiền, yêu cầu hủy chờ duyệt, tin nhắn liên hệ mới).
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int LIST_SIZE = 5;

    private final EntityManager em;
    private final BookingService bookings;
    private final CancellationRequestRepository cancellations;
    private final ContactMessageRepository contacts;
    private final TourRepository tours;
    private final Clock clock;

    public DashboardService(EntityManager em, BookingService bookings, CancellationRequestRepository cancellations,
                            ContactMessageRepository contacts, TourRepository tours, Clock clock) {
        this.em = em;
        this.bookings = bookings;
        this.cancellations = cancellations;
        this.contacts = contacts;
        this.tours = tours;
        this.clock = clock;
    }

    /** Doanh thu là tiền đã thu thật (thanh toán thành công) trừ tiền đã hoàn, tính theo ngày tiền vào/ra. */
    public Dashboard get(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from == null ? today.withDayOfMonth(1) : from;
        LocalDate end = to == null ? today : to;
        if (start.isAfter(end)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        LocalDateTime fromTime = start.atStartOfDay();
        LocalDateTime toTime = end.plusDays(1).atStartOfDay();

        long collected = em.createQuery("""
                        select coalesce(sum(p.amount), 0) from Payment p
                        where p.status = :ok and p.paidAt >= :from and p.paidAt < :to""", Long.class)
                .setParameter("ok", PaymentStatus.SUCCEEDED).setParameter("from", fromTime).setParameter("to", toTime)
                .getSingleResult();
        long refunded = em.createQuery("""
                        select coalesce(sum(r.amount), 0) from Refund r
                        where r.refundedAt >= :from and r.refundedAt < :to""", Long.class)
                .setParameter("from", fromTime).setParameter("to", toTime)
                .getSingleResult();

        Map<BookingStatus, Long> byStatus = new EnumMap<>(BookingStatus.class);
        for (BookingStatus s : BookingStatus.values()) {
            byStatus.put(s, 0L);
        }
        em.createQuery("""
                        select b.status, count(b) from Booking b
                        where b.createdAt >= :from and b.createdAt < :to group by b.status""", Object[].class)
                .setParameter("from", fromTime).setParameter("to", toTime)
                .getResultList()
                .forEach(row -> byStatus.put((BookingStatus) row[0], (Long) row[1]));

        PageResponse<BookingSummary> needRefund = bookings.search(null, null, null, null, true, 0, LIST_SIZE);
        PageResponse<BookingSummary> recent = bookings.search(null, null, null, null, false, 0, LIST_SIZE);
        return new Dashboard(start, end, collected, refunded, collected - refunded, byStatus,
                byStatus.values().stream().mapToLong(Long::longValue).sum(),
                needRefund.total(), cancellations.countByStatus(CancellationRequestStatus.PENDING),
                contacts.countByStatus(ContactStatus.NEW), tours.countByStatus(TourStatus.ON_SALE),
                needRefund.items(), recent.items());
    }

    /**
     * @param bookingsNeedingRefund     số đơn đã thu nhiều hơn số được giữ lại (mọi thời điểm, không theo khoảng ngày)
     * @param bookingsNeedingRefundList tối đa 5 đơn cần hoàn mới nhất
     * @param recentBookings            5 đơn mới nhất
     */
    public record Dashboard(LocalDate from, LocalDate to, long collected, long refunded, long netRevenue,
                            Map<BookingStatus, Long> bookingsByStatus, long newBookings,
                            long bookingsNeedingRefund, long pendingCancellationRequests, long newContactMessages, long toursOnSale,
                            List<BookingSummary> bookingsNeedingRefundList, List<BookingSummary> recentBookings) {}
}
