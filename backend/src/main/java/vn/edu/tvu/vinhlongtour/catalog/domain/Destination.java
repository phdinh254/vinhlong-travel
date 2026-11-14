package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
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
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Điểm đến du lịch (chùa, cù lao, bảo tàng, bãi biển...). Là dữ liệu đầu vào chính của gợi ý lịch trình:
 * vị trí để tính đường đi, giờ mở cửa và ngày đóng cửa, thời gian tham quan, giá vé, độ nổi bật,
 * và các thẻ sở thích (loại hình). Mỗi nhóm thông tin kèm độ tin cậy của dữ liệu.
 */
@Entity
@Table(name = "destinations")
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(length = 300)
    private String address;

    @Embedded
    private GeoPoint location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoordinateQuality coordinateQuality;

    @ManyToMany
    @JoinTable(name = "destination_categories",
            joinColumns = @JoinColumn(name = "destination_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<TourCategory> categories = new HashSet<>();

    @Embedded
    private TimeWindow openingHours;

    @ElementCollection
    @CollectionTable(name = "destination_closed_days", joinColumns = @JoinColumn(name = "destination_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private Set<DayOfWeek> closedDays = EnumSet.noneOf(DayOfWeek.class);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InfoQuality hoursQuality;

    /** Giá vé người lớn (đồng); null = chưa rõ giá. */
    private Long ticketPriceAdult;

    /** Giá vé trẻ em (đồng); null = chưa rõ giá. */
    private Long ticketPriceChild;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InfoQuality priceQuality;

    @Column(nullable = false)
    private int visitMinutes;

    /** Độ nổi bật 1–5, dùng làm một tiêu chí chấm điểm khi gợi ý lịch trình. */
    @Column(nullable = false)
    private int popularity;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String sources;

    @Column(nullable = false)
    private boolean active = true;

    protected Destination() {
    }

    public Destination(String code, String name, Region region) {
        rename(code, name);
        this.region = region;
    }

    public void rename(String code, String name) {
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
    }

    public void describe(String description, String sources) {
        this.description = description;
        this.sources = sources;
    }

    public void locate(Region region, String address, GeoPoint location, CoordinateQuality quality) {
        this.region = region;
        this.address = address;
        this.location = location;
        this.coordinateQuality = quality;
    }

    public void classify(Set<TourCategory> categories) {
        if (categories.isEmpty()) {
            throw ApiException.badRequest("CATEGORY_REQUIRED", "Điểm đến cần ít nhất một loại hình");
        }
        this.categories.clear();
        this.categories.addAll(categories);
    }

    public void schedule(TimeWindow openingHours, Set<DayOfWeek> closedDays, InfoQuality quality) {
        if (closedDays.size() == DayOfWeek.values().length) {
            throw ApiException.badRequest("INVALID_OPENING_HOURS", "Điểm đến không thể đóng cửa cả tuần");
        }
        this.openingHours = openingHours;
        this.closedDays.clear();
        this.closedDays.addAll(closedDays);
        this.hoursQuality = quality;
    }

    public void price(Long adult, Long child, InfoQuality quality) {
        if ((adult != null && adult < 0) || (child != null && child < 0)) {
            throw ApiException.badRequest("INVALID_PRICE", "Giá vé không được âm");
        }
        this.ticketPriceAdult = adult;
        this.ticketPriceChild = child;
        this.priceQuality = quality;
    }

    public void rate(int visitMinutes, int popularity) {
        if (visitMinutes <= 0 || popularity < 1 || popularity > 5) {
            throw ApiException.badRequest("INVALID_RATING", "Thời gian tham quan phải lớn hơn 0 và độ nổi bật từ 1 đến 5");
        }
        this.visitMinutes = visitMinutes;
        this.popularity = popularity;
    }

    /** Ngừng hiển thị cho khách nhưng vẫn giữ dữ liệu (lịch trình cũ vẫn tham chiếu được). */
    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Region getRegion() {
        return region;
    }

    public String getAddress() {
        return address;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public CoordinateQuality getCoordinateQuality() {
        return coordinateQuality;
    }

    public Set<TourCategory> getCategories() {
        return Set.copyOf(categories);
    }

    public TimeWindow getOpeningHours() {
        return openingHours;
    }

    public Set<DayOfWeek> getClosedDays() {
        return closedDays.isEmpty() ? Set.of() : EnumSet.copyOf(closedDays);
    }

    public InfoQuality getHoursQuality() {
        return hoursQuality;
    }

    public Long getTicketPriceAdult() {
        return ticketPriceAdult;
    }

    public Long getTicketPriceChild() {
        return ticketPriceChild;
    }

    public InfoQuality getPriceQuality() {
        return priceQuality;
    }

    public int getVisitMinutes() {
        return visitMinutes;
    }

    public int getPopularity() {
        return popularity;
    }

    public String getDescription() {
        return description;
    }

    public String getSources() {
        return sources;
    }

    public boolean isActive() {
        return active;
    }
}
