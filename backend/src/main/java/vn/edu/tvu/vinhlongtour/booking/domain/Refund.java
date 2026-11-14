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
 * Một lần admin hoàn tiền cho khách (chuyển khoản/tiền mặt, kèm chứng từ). Một đơn có thể hoàn nhiều lần.
 * Chỉ được tạo qua {@link Booking#refund}, không hoàn quá số tiền cần hoàn.
 */
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancellation_request_id")
    private CancellationRequest cancellationRequest;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(length = 50)
    private String transactionNo;

    @Column(length = 500)
    private String proofUrl;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime refundedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refunded_by")
    private Account refundedBy;

    protected Refund() {
    }

    Refund(Booking booking, CancellationRequest request, long amount, PaymentMethod method, String transactionNo,
           String proofUrl, String note, Account admin, LocalDateTime now) {
        this.booking = booking;
        this.cancellationRequest = request;
        this.amount = amount;
        this.method = method;
        this.transactionNo = transactionNo;
        this.proofUrl = proofUrl;
        this.note = note;
        this.refundedBy = admin;
        this.refundedAt = now;
    }

    public Long getId() {
        return id;
    }

    public CancellationRequest getCancellationRequest() {
        return cancellationRequest;
    }

    public long getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public Account getRefundedBy() {
        return refundedBy;
    }
}
