package vn.edu.tvu.vinhlongtour.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;

import java.time.LocalDateTime;

/**
 * Một lần thanh toán của đơn: qua VNPay (tạo ở trạng thái chờ, VNPay báo kết quả sau)
 * hoặc admin xác nhận chuyển khoản/tiền mặt (thành công ngay, kèm ảnh chứng từ).
 * Kế thừa {@link MoneyTransaction}: tiền đi vào, chỉ tính là đã chuyển khi thành công.
 * Chỉ được tạo qua {@link Booking}; tiền chỉ cộng vào đơn khi thanh toán chuyển sang thành công.
 */
@Entity
@Table(name = "payments")
public class Payment extends MoneyTransaction {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(unique = true, length = 100)
    private String reference;

    @Column(length = 20)
    private String bankCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Account confirmedBy;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 2000)
    private String rawResponse;

    protected Payment() {
    }

    private Payment(Booking booking, long amount, PaymentMethod method, String transactionNo, String proofUrl, String note,
                    PaymentStatus status, LocalDateTime createdAt) {
        super(booking, amount, method, transactionNo, proofUrl, note);
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Thanh toán qua VNPay đang chờ kết quả. */
    static Payment online(Booking booking, long amount, LocalDateTime now) {
        return new Payment(booking, amount, PaymentMethod.VNPAY, null, null, null, PaymentStatus.PENDING, now);
    }

    /** Admin xác nhận đã nhận tiền chuyển khoản/tiền mặt. */
    static Payment manual(Booking booking, long amount, PaymentMethod method, String transactionNo, String proofUrl,
                          String note, Account admin, LocalDateTime now) {
        Payment p = new Payment(booking, amount, method, transactionNo, proofUrl, note, PaymentStatus.SUCCEEDED, now);
        p.confirmedBy = admin;
        p.paidAt = now;
        return p;
    }

    @Override
    public MoneyDirection direction() {
        return MoneyDirection.IN;
    }

    @Override
    public boolean isSettled() {
        return status == PaymentStatus.SUCCEEDED;
    }

    @Override
    public LocalDateTime settledAt() {
        return paidAt;
    }

    @Override
    public Account handledBy() {
        return confirmedBy;
    }

    /** Mã tham chiếu gửi sang VNPay (vnp_TxnRef), gán sau khi đã có mã thanh toán trong CSDL. */
    public void assignReference(String reference) {
        this.reference = reference;
    }

    void markSucceeded(String transactionNo, String bankCode, LocalDateTime paidAt, String rawResponse) {
        this.status = PaymentStatus.SUCCEEDED;
        recordTransactionNo(transactionNo);
        this.bankCode = bankCode;
        this.paidAt = paidAt;
        this.rawResponse = rawResponse;
    }

    void markFailed(String rawResponse) {
        this.status = PaymentStatus.FAILED;
        this.rawResponse = rawResponse;
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public String getBankCode() {
        return bankCode;
    }

    public Account getConfirmedBy() {
        return confirmedBy;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
