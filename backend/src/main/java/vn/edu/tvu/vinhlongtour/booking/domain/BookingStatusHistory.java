package vn.edu.tvu.vinhlongtour.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;

import java.time.LocalDateTime;

/**
 * Một lần đổi trạng thái đơn: từ đâu sang đâu, lúc nào, ai làm (null = hệ thống tự động), lý do.
 * Chỉ được tạo qua {@link Booking}, không sửa sau khi tạo.
 */
@Entity
@Table(name = "booking_status_history")
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus toStatus;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private Account changedBy;

    @Column(length = 500)
    private String reason;

    protected BookingStatusHistory() {
    }

    BookingStatusHistory(Booking booking, BookingStatus fromStatus, BookingStatus toStatus,
                         LocalDateTime changedAt, Account changedBy, String reason) {
        this.booking = booking;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.reason = reason;
    }

    public BookingStatus getFromStatus() {
        return fromStatus;
    }

    public BookingStatus getToStatus() {
        return toStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public Account getChangedBy() {
        return changedBy;
    }

    public String getReason() {
        return reason;
    }
}
