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
 * Một lần thanh toán của đơn: qua VNPay (tạo ở trạng thái chờ, VNPay báo kết quả sau)
 * hoặc admin xác nhận chuyển khoản/tiền mặt (thành công ngay, kèm ảnh chứng từ).
 * Chỉ được tạo qua {@link Booking}; tiền chỉ cộng vào đơn khi thanh toán chuyển sang thành công.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(unique = true, length = 100)
    private String reference;

    @Column(length = 50)
    private String transactionNo;

    @Column(length = 20)
    private String bankCode;

    @Column(length = 500)
    private String proofUrl;

    @Column(length = 500)
    private String note;

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

    /** Thanh toán qua VNPay đang chờ kết quả. */
    static Payment online(Booking booking, long amount, LocalDateTime now) {
        Payment p = new Payment();
        p.booking = booking;
        p.amount = amount;
        p.method = PaymentMethod.VNPAY;
        p.status = PaymentStatus.PENDING;
        p.createdAt = now;
        return p;
    }

    /** Admin xác nhận đã nhận tiền chuyển khoản/tiền mặt. */
    static Payment manual(Booking booking, long amount, PaymentMethod method, String transactionNo, String proofUrl,
                          String note, Account admin, LocalDateTime now) {
        Payment p = new Payment();
        p.booking = booking;
        p.amount = amount;
        p.method = method;
        p.status = PaymentStatus.SUCCEEDED;
        p.transactionNo = transactionNo;
        p.proofUrl = proofUrl;
        p.note = note;
        p.confirmedBy = admin;
        p.paidAt = now;
        p.createdAt = now;
        return p;
    }

    /** Mã tham chiếu gửi sang VNPay (vnp_TxnRef), gán sau khi đã có mã thanh toán trong CSDL. */
    public void assignReference(String reference) {
        this.reference = reference;
    }

    void markSucceeded(String transactionNo, String bankCode, LocalDateTime paidAt, String rawResponse) {
        this.status = PaymentStatus.SUCCEEDED;
        this.transactionNo = transactionNo;
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

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public long getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public String getNote() {
        return note;
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
