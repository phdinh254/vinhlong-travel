package vn.edu.tvu.vinhlongtour.booking.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.catalog.domain.CancellationPolicy;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn đặt tour: một đợt khởi hành, người liên hệ (khách vãng lai hoặc có tài khoản), danh sách hành khách,
 * phụ thu đã chọn, các khoản tiền (chốt lúc đặt) và trạng thái.
 * Mọi lần đổi trạng thái đều qua {@link #moveTo} để kiểm tra bước chuyển hợp lệ và ghi lịch sử.
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departure_id")
    private Departure departure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(nullable = false, length = 150)
    private String contactName;

    @Column(nullable = false, length = 20)
    private String contactPhone;

    @Column(length = 150)
    private String contactEmail;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private long ticketTotal;

    @Column(nullable = false)
    private long pickupTotal;

    @Column(nullable = false)
    private long surchargeTotal;

    @Column(nullable = false)
    private long totalAmount;

    /** Số tiền thực thu (đã trừ các lần hoàn tiền). */
    @Column(nullable = false)
    private long paidAmount;

    @Column(nullable = false)
    private int seats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    private LocalDateTime holdUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancellation_policy_id")
    private CancellationPolicy cancellationPolicy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Passenger> passengers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "booking_surcharges", joinColumns = @JoinColumn(name = "booking_id"))
    private List<BookingSurcharge> surcharges = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<BookingStatusHistory> history = new ArrayList<>();

    protected Booking() {
    }

    /**
     * Tạo đơn mới đang giữ chỗ. Đơn 0 đồng (hiếm, ví dụ tour miễn phí) coi như đã thanh toán ngay.
     */
    public Booking(String code, Departure departure, Account account, Contact contact, List<Passenger.Details> passengerDetails,
                   List<BookingSurcharge> surcharges, CancellationPolicy policy, LocalDateTime now, LocalDateTime holdUntil) {
        this.code = code;
        this.departure = departure;
        this.account = account;
        this.contactName = contact.name().trim();
        this.contactPhone = contact.phone().trim();
        this.contactEmail = contact.email();
        this.note = contact.note();
        passengerDetails.forEach(d -> passengers.add(new Passenger(this, d)));
        this.surcharges.addAll(surcharges);
        this.ticketTotal = passengerDetails.stream().mapToLong(Passenger.Details::price).sum();
        this.pickupTotal = passengerDetails.stream().mapToLong(Passenger.Details::pickupFee).sum();
        this.surchargeTotal = surcharges.stream().mapToLong(BookingSurcharge::getAmount).sum();
        this.totalAmount = ticketTotal + pickupTotal + surchargeTotal;
        this.seats = passengerDetails.stream().mapToInt(d -> d.passengerType().getSeats()).sum();
        this.cancellationPolicy = policy;
        this.createdAt = now;
        this.holdUntil = holdUntil;
        this.status = BookingStatus.PENDING_PAYMENT;
        history.add(new BookingStatusHistory(this, null, status, now, account, "Tạo đơn"));
        if (totalAmount == 0) {
            moveTo(BookingStatus.PAID, null, "Đơn 0 đồng, không cần thanh toán", now);
        }
    }

    /** Thông tin người liên hệ của đơn. */
    public record Contact(String name, String phone, String email, String note) {}

    /**
     * Đổi trạng thái kèm ghi lịch sử.
     *
     * @param actor tài khoản thực hiện; null = hệ thống tự động
     */
    public void moveTo(BookingStatus target, Account actor, String reason, LocalDateTime at) {
        if (!status.canMoveTo(target)) {
            throw ApiException.conflict("INVALID_STATUS_CHANGE", "Đơn đang ở trạng thái " + status + ", không thể chuyển sang " + target);
        }
        history.add(new BookingStatusHistory(this, status, target, at, actor, reason));
        this.status = target;
        if (target != BookingStatus.PENDING_PAYMENT) {
            this.holdUntil = null; // hạn giữ chỗ chỉ có nghĩa khi đang chờ thanh toán
        }
    }

    /** Đơn chưa trả đồng nào và đã quá hạn giữ chỗ. */
    public boolean isHoldExpired(LocalDateTime now) {
        return status == BookingStatus.PENDING_PAYMENT && paidAmount == 0 && holdUntil != null && holdUntil.isBefore(now);
    }

    public long remainingAmount() {
        return Math.max(0, totalAmount - paidAmount);
    }

    /** Khách vãng lai xem đơn bằng mã đơn + số điện thoại người liên hệ. */
    public boolean matchesPhone(String phone) {
        return phone != null && contactPhone.equals(phone.trim());
    }

    public boolean belongsTo(Long accountId) {
        return account != null && account.getId().equals(accountId);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Departure getDeparture() {
        return departure;
    }

    public Account getAccount() {
        return account;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getNote() {
        return note;
    }

    public long getTicketTotal() {
        return ticketTotal;
    }

    public long getPickupTotal() {
        return pickupTotal;
    }

    public long getSurchargeTotal() {
        return surchargeTotal;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public long getPaidAmount() {
        return paidAmount;
    }

    public int getSeats() {
        return seats;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getHoldUntil() {
        return holdUntil;
    }

    public CancellationPolicy getCancellationPolicy() {
        return cancellationPolicy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Passenger> getPassengers() {
        return List.copyOf(passengers);
    }

    public List<BookingSurcharge> getSurcharges() {
        return List.copyOf(surcharges);
    }

    public List<BookingStatusHistory> getHistory() {
        return List.copyOf(history);
    }
}
