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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.catalog.domain.Region;
import vn.edu.tvu.vinhlongtour.catalog.domain.Surcharge;
import vn.edu.tvu.vinhlongtour.catalog.domain.TourCategory;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tour du lịch: thông tin giới thiệu, khu vực, loại hình, dịch vụ bao gồm/không bao gồm,
 * phụ thu áp dụng, bộ ảnh và lịch trình mẫu. Giá và ngày đi nằm ở từng {@link Departure}.
 * Tour chỉ được chuyển sang đang bán khi đủ thông tin (xem {@link #ensureSellable()}).
 */
@Entity
@Table(name = "tours")
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String summary;

    @Column(length = 5000)
    private String description;

    @Column(nullable = false)
    private int durationDays;

    @Column(nullable = false)
    private int durationNights;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TourStatus status = TourStatus.DRAFT;

    @Column(nullable = false)
    private boolean featured;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary = new Itinerary();

    @ManyToMany
    @JoinTable(name = "tour_regions", joinColumns = @JoinColumn(name = "tour_id"), inverseJoinColumns = @JoinColumn(name = "region_id"))
    private Set<Region> regions = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "tour_category_links", joinColumns = @JoinColumn(name = "tour_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<TourCategory> categories = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "tour_amenities", joinColumns = @JoinColumn(name = "tour_id"))
    private List<TourAmenity> amenities = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "tour_surcharges", joinColumns = @JoinColumn(name = "tour_id"), inverseJoinColumns = @JoinColumn(name = "surcharge_id"))
    private Set<Surcharge> surcharges = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "tour_images", joinColumns = @JoinColumn(name = "tour_id"))
    @OrderColumn(name = "sort_order")
    private List<TourImage> images = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Tour() {
    }

    public Tour(String code, String name, int durationDays, int durationNights) {
        describe(code, name, null, null, durationDays, durationNights);
    }

    public void describe(String code, String name, String summary, String description, int durationDays, int durationNights) {
        if (durationDays < 1 || durationNights < 0 || durationNights > durationDays) {
            throw ApiException.badRequest("INVALID_DURATION", "Số ngày phải từ 1, số đêm từ 0 và không quá số ngày");
        }
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.summary = summary;
        this.description = description;
        this.durationDays = durationDays;
        this.durationNights = durationNights;
    }

    public void classify(Set<Region> regions, Set<TourCategory> categories) {
        if (regions.isEmpty() || categories.isEmpty()) {
            throw ApiException.badRequest("CLASSIFICATION_REQUIRED", "Tour cần ít nhất một khu vực và một loại hình");
        }
        this.regions.clear();
        this.regions.addAll(regions);
        this.categories.clear();
        this.categories.addAll(categories);
    }

    public void setAmenities(List<TourAmenity> amenities) {
        if (amenities.stream().map(a -> a.getAmenity().getId()).distinct().count() != amenities.size()) {
            throw ApiException.badRequest("DUPLICATE_AMENITY", "Mỗi dịch vụ chỉ được khai báo một lần");
        }
        this.amenities.clear();
        this.amenities.addAll(amenities);
    }

    public void setSurcharges(Set<Surcharge> surcharges) {
        this.surcharges.clear();
        this.surcharges.addAll(surcharges);
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    /**
     * Thay bộ ảnh. Luôn giữ đúng một ảnh đại diện: không chọn ảnh nào thì ảnh đầu tiên làm đại diện,
     * chọn nhiều hơn một thì báo lỗi.
     */
    public void replaceImages(List<TourImage> newImages) {
        long covers = newImages.stream().filter(TourImage::cover).count();
        if (covers > 1) {
            throw ApiException.badRequest("MULTIPLE_COVERS", "Chỉ được chọn một ảnh đại diện");
        }
        List<TourImage> result = new ArrayList<>(newImages);
        if (covers == 0 && !result.isEmpty()) {
            TourImage first = result.getFirst();
            result.set(0, new TourImage(first.url(), first.caption(), true));
        }
        images.clear();
        images.addAll(result);
        if (status == TourStatus.ON_SALE) {
            ensureSellable();
        }
    }

    /** Thay lịch trình mẫu. Các đợt đã tạo giữ bản sao riêng nên không bị ảnh hưởng. */
    public void replaceItinerary(List<Itinerary.DayPlan> plans) {
        itinerary.replaceDays(plans);
        if (status == TourStatus.ON_SALE) {
            ensureSellable();
        }
    }

    public void changeStatus(TourStatus newStatus) {
        if (newStatus == TourStatus.ON_SALE) {
            ensureSellable();
        }
        this.status = newStatus;
    }

    /** Tour đang bán phải có ảnh đại diện và lịch trình mẫu đủ đúng số ngày. */
    public void ensureSellable() {
        if (coverImageUrl() == null) {
            throw ApiException.badRequest("TOUR_INCOMPLETE", "Tour cần ít nhất một ảnh trước khi mở bán");
        }
        if (itinerary.dayCount() != durationDays) {
            throw ApiException.badRequest("TOUR_INCOMPLETE",
                    "Lịch trình mẫu có " + itinerary.dayCount() + " ngày nhưng tour dài " + durationDays + " ngày");
        }
    }

    public boolean isOnSale() {
        return status == TourStatus.ON_SALE;
    }

    public String coverImageUrl() {
        return images.stream().filter(TourImage::cover).map(TourImage::url).findFirst().orElse(null);
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

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public int getDurationNights() {
        return durationNights;
    }

    public TourStatus getStatus() {
        return status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public Set<Region> getRegions() {
        return Set.copyOf(regions);
    }

    public Set<TourCategory> getCategories() {
        return Set.copyOf(categories);
    }

    public List<TourAmenity> getAmenities() {
        return List.copyOf(amenities);
    }

    public Set<Surcharge> getSurcharges() {
        return Set.copyOf(surcharges);
    }

    public List<TourImage> getImages() {
        return List.copyOf(images);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
