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
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.catalog.domain.PickupPoint;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một hành khách đi tour (có thể khác người đặt). Giá vé và phí đón được chốt lúc đặt,
 * nên admin đổi bảng giá của đợt sau đó không làm thay đổi đơn cũ.
 */
@Entity
@Table(name = "booking_passengers")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 20)
    private String phone;

    @Column(length = 30)
    private String idNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_type_id")
    private PassengerType passengerType;

    @Column(nullable = false)
    private long price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_point_id")
    private PickupPoint pickupPoint;

    private LocalTime pickupTime;

    @Column(nullable = false)
    private long pickupFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PassengerStatus status = PassengerStatus.ACTIVE;

    protected Passenger() {
    }

    Passenger(Booking booking, Details details) {
        this.booking = booking;
        this.fullName = details.fullName().trim();
        this.birthDate = details.birthDate();
        this.gender = details.gender();
        this.phone = details.phone();
        this.idNumber = details.idNumber();
        this.passengerType = details.passengerType();
        this.price = details.price();
        this.pickupPoint = details.pickupPoint();
        this.pickupTime = details.pickupTime();
        this.pickupFee = details.pickupFee();
    }

    void cancel() {
        this.status = PassengerStatus.CANCELLED;
    }

    /** Tính lại giá khi chuyển đợt: loại khách (theo tuổi ngày đi mới), giá vé và điểm đón theo đợt mới. */
    void reprice(Details d) {
        this.passengerType = d.passengerType();
        this.price = d.price();
        this.pickupPoint = d.pickupPoint();
        this.pickupTime = d.pickupTime();
        this.pickupFee = d.pickupFee();
    }

    /** Dữ liệu một hành khách khi tạo đơn (đã tính giá). */
    public record Details(String fullName, LocalDate birthDate, Gender gender, String phone, String idNumber,
                          PassengerType passengerType, long price, PickupPoint pickupPoint, LocalTime pickupTime, long pickupFee) {}

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public PassengerType getPassengerType() {
        return passengerType;
    }

    public long getPrice() {
        return price;
    }

    public PickupPoint getPickupPoint() {
        return pickupPoint;
    }

    public LocalTime getPickupTime() {
        return pickupTime;
    }

    public long getPickupFee() {
        return pickupFee;
    }

    public PassengerStatus getStatus() {
        return status;
    }
}
