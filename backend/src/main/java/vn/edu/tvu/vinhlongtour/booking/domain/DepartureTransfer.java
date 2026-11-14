package vn.edu.tvu.vinhlongtour.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;

import java.time.LocalDateTime;

/**
 * Lịch sử chuyển đơn sang đợt khởi hành khác của cùng tour, kèm chênh lệch tiền sau khi tính lại giá
 * (dương: khách phải trả thêm; âm: được hoàn bớt).
 */
@Entity
@Table(name = "departure_transfers")
public class DepartureTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_departure_id")
    private Departure fromDeparture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_departure_id")
    private Departure toDeparture;

    @Column(nullable = false)
    private long priceDifference;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime transferredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transferred_by")
    private Account transferredBy;

    protected DepartureTransfer() {
    }

    DepartureTransfer(Booking booking, Departure from, Departure to, long priceDifference, String reason, Account admin, LocalDateTime now) {
        this.booking = booking;
        this.fromDeparture = from;
        this.toDeparture = to;
        this.priceDifference = priceDifference;
        this.reason = reason;
        this.transferredBy = admin;
        this.transferredAt = now;
    }

    public Departure getFromDeparture() {
        return fromDeparture;
    }

    public Departure getToDeparture() {
        return toDeparture;
    }

    public long getPriceDifference() {
        return priceDifference;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }

    public Account getTransferredBy() {
        return transferredBy;
    }
}
