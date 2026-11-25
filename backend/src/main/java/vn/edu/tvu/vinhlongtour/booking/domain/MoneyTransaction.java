package vn.edu.tvu.vinhlongtour.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;

import java.time.LocalDateTime;

/**
 * Lớp cha trừu tượng của mọi khoản tiền gắn với một đơn: khách thanh toán ({@link Payment}) và công ty hoàn tiền
 * ({@link Refund}). Giữ các trường chung (số tiền, phương thức, mã giao dịch, ảnh chứng từ, ghi chú); mỗi lớp con
 * tự cho biết tiền đi chiều nào, đã thực sự chuyển chưa, lúc nào và ai xử lý. Nhờ vậy {@link Booking#moneyHistory()}
 * gộp hai loại thành một sổ tiền mà không cần hỏi "đây là thanh toán hay hoàn tiền".
 * Mỗi lớp con vẫn là một bảng riêng ({@code payments}, {@code refunds}).
 */
@MappedSuperclass
public abstract class MoneyTransaction {

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

    @Column(length = 50)
    private String transactionNo;

    @Column(length = 500)
    private String proofUrl;

    @Column(length = 500)
    private String note;

    protected MoneyTransaction() {
    }

    protected MoneyTransaction(Booking booking, long amount, PaymentMethod method, String transactionNo, String proofUrl, String note) {
        this.booking = booking;
        this.amount = amount;
        this.method = method;
        this.transactionNo = transactionNo;
        this.proofUrl = proofUrl;
        this.note = note;
    }

    /** Tiền khách trả vào hay công ty hoàn ra. */
    public abstract MoneyDirection direction();

    /** Tiền đã thực sự chuyển (thanh toán còn chờ hoặc thất bại thì chưa). */
    public abstract boolean isSettled();

    /** Lúc tiền chuyển xong; {@code null} khi chưa xong. */
    public abstract LocalDateTime settledAt();

    /** Admin xác nhận/thực hiện; {@code null} khi hệ thống tự ghi nhận (payOS). */
    public abstract Account handledBy();

    /** Số tiền có dấu: dương là tiền vào, âm là tiền ra. Cộng cả sổ tiền ra đúng số tiền công ty đang giữ của đơn. */
    public long signedAmount() {
        return direction() == MoneyDirection.IN ? amount : -amount;
    }

    protected void recordTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
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

    public String getTransactionNo() {
        return transactionNo;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public String getNote() {
        return note;
    }
}
