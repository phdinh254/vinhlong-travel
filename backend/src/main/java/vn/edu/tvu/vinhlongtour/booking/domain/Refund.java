package vn.edu.tvu.vinhlongtour.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;

import java.time.LocalDateTime;

/**
 * Một lần admin hoàn tiền cho khách (chuyển khoản/tiền mặt, kèm chứng từ). Một đơn có thể hoàn nhiều lần.
 * Kế thừa {@link MoneyTransaction}: tiền đi ra, đã chuyển ngay khi admin ghi nhận.
 * Chỉ được tạo qua {@link Booking#refund}, không hoàn quá số tiền cần hoàn.
 */
@Entity
@Table(name = "refunds")
public class Refund extends MoneyTransaction {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancellation_request_id")
    private CancellationRequest cancellationRequest;

    @Column(nullable = false)
    private LocalDateTime refundedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refunded_by")
    private Account refundedBy;

    protected Refund() {
    }

    Refund(Booking booking, CancellationRequest request, long amount, PaymentMethod method, String transactionNo,
           String proofUrl, String note, Account admin, LocalDateTime now) {
        super(booking, amount, method, transactionNo, proofUrl, note);
        this.cancellationRequest = request;
        this.refundedBy = admin;
        this.refundedAt = now;
    }

    @Override
    public MoneyDirection direction() {
        return MoneyDirection.OUT;
    }

    @Override
    public boolean isSettled() {
        return true;
    }

    @Override
    public LocalDateTime settledAt() {
        return refundedAt;
    }

    @Override
    public Account handledBy() {
        return refundedBy;
    }

    public CancellationRequest getCancellationRequest() {
        return cancellationRequest;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public Account getRefundedBy() {
        return refundedBy;
    }
}
