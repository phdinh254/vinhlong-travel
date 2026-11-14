package vn.edu.tvu.vinhlongtour.tour.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Một đợt khởi hành của tour: ngày giờ đi/về, hạn nhận khách, sức chứa và số chỗ đã đặt,
 * bảng giá theo loại khách, các điểm đón, và lịch trình riêng (sao chép từ lịch trình mẫu khi tạo).
 * Tự giữ quy tắc của mình: không giảm sức chứa xuống dưới số chỗ đã đặt, không hủy/xóa khi đã có khách.
 */
@Entity
@Table(name = "departures")
public class Departure {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd-HHmm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false)
    private LocalDateTime departureAt;

    @Column(nullable = false)
    private LocalDateTime returnAt;

    @Column(nullable = false)
    private LocalDateTime bookingDeadline;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int bookedSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartureStatus status = DepartureStatus.OPEN;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    @ElementCollection
    @CollectionTable(name = "departure_prices", joinColumns = @JoinColumn(name = "departure_id"))
    private List<DeparturePrice> prices = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "departure_pickups", joinColumns = @JoinColumn(name = "departure_id"))
    private List<DeparturePickup> pickups = new ArrayList<>();

    @Version
    private long version;

    protected Departure() {
    }

    /** Tạo đợt mới với lịch trình sao chép từ lịch trình mẫu của tour. Mã đợt = mã tour + ngày giờ đi. */
    public Departure(Tour tour, LocalDateTime departureAt, LocalDateTime returnAt, LocalDateTime bookingDeadline,
                     int capacity, LocalDateTime now) {
        if (!departureAt.isAfter(now)) {
            throw ApiException.badRequest("DEPARTURE_IN_PAST", "Ngày khởi hành phải ở tương lai");
        }
        this.tour = tour;
        this.itinerary = tour.getItinerary().copy();
        schedule(departureAt, returnAt, bookingDeadline);
        this.code = tour.getCode() + "-" + departureAt.format(CODE_DATE);
        changeCapacity(capacity);
    }

    public void schedule(LocalDateTime departureAt, LocalDateTime returnAt, LocalDateTime bookingDeadline) {
        if (!departureAt.isBefore(returnAt)) {
            throw ApiException.badRequest("INVALID_SCHEDULE", "Giờ về phải sau giờ đi");
        }
        if (bookingDeadline.isAfter(departureAt)) {
            throw ApiException.badRequest("INVALID_SCHEDULE", "Hạn nhận khách không được sau giờ khởi hành");
        }
        this.departureAt = departureAt;
        this.returnAt = returnAt;
        this.bookingDeadline = bookingDeadline;
    }

    public void changeCapacity(int capacity) {
        if (capacity < 1 || capacity < bookedSeats) {
            throw ApiException.badRequest("INVALID_CAPACITY", "Sức chứa phải từ 1 và không nhỏ hơn số chỗ đã đặt (" + bookedSeats + ")");
        }
        this.capacity = capacity;
    }

    /** Bảng giá phải có đúng một giá cho mỗi loại khách, để hành khách nào cũng tính được giá. */
    public void setPrices(List<DeparturePrice> newPrices, Collection<PassengerType> allTypes) {
        List<Long> typeIds = newPrices.stream().map(p -> p.getPassengerType().getId()).toList();
        if (typeIds.stream().distinct().count() != typeIds.size()) {
            throw ApiException.badRequest("DUPLICATE_PRICE", "Mỗi loại khách chỉ có một giá");
        }
        allTypes.stream().filter(t -> !typeIds.contains(t.getId())).findFirst().ifPresent(t -> {
            throw ApiException.badRequest("MISSING_PRICE", "Chưa có giá cho loại khách \"" + t.getName() + "\"");
        });
        if (newPrices.stream().anyMatch(p -> p.getPrice() < 0)) {
            throw ApiException.badRequest("INVALID_PRICE", "Giá không được âm");
        }
        prices.clear();
        prices.addAll(newPrices);
    }

    public void setPickups(List<DeparturePickup> newPickups) {
        if (newPickups.stream().map(p -> p.getPickupPoint().getId()).distinct().count() != newPickups.size()) {
            throw ApiException.badRequest("DUPLICATE_PICKUP", "Mỗi điểm đón chỉ khai báo một lần");
        }
        if (newPickups.stream().anyMatch(p -> !p.getPickupPoint().isActive() || p.getFee() < 0)) {
            throw ApiException.badRequest("INVALID_PICKUP", "Điểm đón phải đang hoạt động và phí đón không được âm");
        }
        pickups.clear();
        pickups.addAll(newPickups);
    }

    /** Sửa lịch trình riêng của đợt (ví dụ đổi giờ do nước lên), không ảnh hưởng lịch trình mẫu của tour. */
    public void replaceItinerary(List<Itinerary.DayPlan> plans) {
        itinerary.replaceDays(plans);
    }

    public void changeStatus(DepartureStatus newStatus) {
        if (newStatus == DepartureStatus.CANCELLED) {
            ensureNoBookings();
        }
        this.status = newStatus;
    }

    public void ensureNoBookings() {
        if (bookedSeats > 0) {
            throw ApiException.conflict("DEPARTURE_HAS_BOOKINGS", "Đợt đã có " + bookedSeats + " chỗ được đặt, không thể hủy hoặc xóa");
        }
    }

    /** Giữ chỗ cho một đơn. Phải gọi khi đã khóa dòng đợt này trong CSDL (xem DepartureRepository.findByIdForUpdate). */
    public void reserveSeats(int seats) {
        if (seats > seatsLeft()) {
            throw ApiException.conflict("SEAT_NOT_ENOUGH", "Đợt khởi hành chỉ còn " + seatsLeft() + " chỗ");
        }
        this.bookedSeats += seats;
    }

    /** Trả chỗ khi đơn hết hạn hoặc bị hủy. Phải gọi khi đã khóa dòng đợt này. */
    public void releaseSeats(int seats) {
        if (seats > bookedSeats) {
            throw new IllegalStateException("Trả " + seats + " chỗ nhưng đợt " + code + " chỉ có " + bookedSeats + " chỗ đã đặt");
        }
        this.bookedSeats -= seats;
    }

    public Optional<DeparturePrice> priceFor(PassengerType type) {
        return prices.stream().filter(p -> p.getPassengerType().getId().equals(type.getId())).findFirst();
    }

    public Optional<DeparturePickup> pickupAt(Long pickupPointId) {
        return pickups.stream().filter(p -> p.getPickupPoint().getId().equals(pickupPointId)).findFirst();
    }

    public int seatsLeft() {
        return capacity - bookedSeats;
    }

    /** Khách đặt được khi tour đang bán, đợt đang mở, chưa quá hạn nhận khách và còn chỗ. */
    public boolean isBookable(LocalDateTime now) {
        return tour.isOnSale() && status == DepartureStatus.OPEN && now.isBefore(bookingDeadline) && seatsLeft() > 0;
    }

    public Long getId() {
        return id;
    }

    public Tour getTour() {
        return tour;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public LocalDateTime getReturnAt() {
        return returnAt;
    }

    public LocalDateTime getBookingDeadline() {
        return bookingDeadline;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getBookedSeats() {
        return bookedSeats;
    }

    public DepartureStatus getStatus() {
        return status;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public List<DeparturePrice> getPrices() {
        return List.copyOf(prices);
    }

    public List<DeparturePickup> getPickups() {
        return List.copyOf(pickups);
    }
}
