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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Yêu cầu hủy tour của khách: hủy cả đơn hoặc một số hành khách. Phần trăm phạt và số tiền phạt
 * được chốt ngay lúc gửi yêu cầu, nên admin duyệt muộn cũng không làm khách bị phạt nặng hơn.
 * Chỉ được tạo và đổi trạng thái qua {@link Booking}.
 */
@Entity
@Table(name = "cancellation_requests")
public class CancellationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CancellationScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CancellationRequestStatus status = CancellationRequestStatus.PENDING;

    @Column(length = 1000)
    private String reason;

    @ManyToMany
    @JoinTable(name = "cancellation_request_passengers",
            joinColumns = @JoinColumn(name = "request_id"), inverseJoinColumns = @JoinColumn(name = "passenger_id"))
    private List<Passenger> passengers = new ArrayList<>();

    @Column(nullable = false)
    private int penaltyPercent;

    @Column(nullable = false)
    private long penaltyAmount;

    @Column(nullable = false)
    private long estimatedRefund;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private Account requestedBy;

    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private Account decidedBy;

    @Column(length = 500)
    private String decisionNote;

    protected CancellationRequest() {
    }

    CancellationRequest(Booking booking, CancellationScope scope, List<Passenger> passengers, String reason,
                        int penaltyPercent, long penaltyAmount, long estimatedRefund, Account requestedBy, LocalDateTime now) {
        this.booking = booking;
        this.scope = scope;
        this.passengers.addAll(passengers);
        this.reason = reason;
        this.penaltyPercent = penaltyPercent;
        this.penaltyAmount = penaltyAmount;
        this.estimatedRefund = estimatedRefund;
        this.requestedBy = requestedBy;
        this.requestedAt = now;
    }

    void decide(CancellationRequestStatus result, Account admin, String note, LocalDateTime now) {
        this.status = result;
        this.decidedBy = admin;
        this.decisionNote = note;
        this.decidedAt = now;
    }

    public boolean isPending() {
        return status == CancellationRequestStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public CancellationScope getScope() {
        return scope;
    }

    public CancellationRequestStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public List<Passenger> getPassengers() {
        return List.copyOf(passengers);
    }

    public int getPenaltyPercent() {
        return penaltyPercent;
    }

    public long getPenaltyAmount() {
        return penaltyAmount;
    }

    public long getEstimatedRefund() {
        return estimatedRefund;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public Account getRequestedBy() {
        return requestedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public Account getDecidedBy() {
        return decidedBy;
    }

    public String getDecisionNote() {
        return decisionNote;
    }
}
