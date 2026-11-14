package vn.edu.tvu.vinhlongtour.tour.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.catalog.repository.AmenityRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.RegionRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.SurchargeRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.TourCategoryRepository;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.Lookups;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.tour.domain.DepartureStatus;
import vn.edu.tvu.vinhlongtour.tour.domain.Tour;
import vn.edu.tvu.vinhlongtour.tour.domain.TourAmenity;
import vn.edu.tvu.vinhlongtour.tour.domain.TourImage;
import vn.edu.tvu.vinhlongtour.tour.domain.TourStatus;
import vn.edu.tvu.vinhlongtour.tour.repository.DepartureRepository;
import vn.edu.tvu.vinhlongtour.tour.repository.DepartureRepository.TourPriceSummary;
import vn.edu.tvu.vinhlongtour.tour.repository.TourRepository;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.AmenityLine;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ImagesRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ItineraryRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourCard;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourDetail;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ tour: khách tìm/lọc và xem tour đang bán; admin tạo, sửa, đổi ảnh, đổi lịch trình mẫu, xóa.
 */
@Service
@Transactional
public class TourService {

    /**
     * Điều kiện tìm tour. Trường nào null thì không lọc theo trường đó.
     *
     * @param onSaleOnly true khi khách tìm (chỉ thấy tour đang bán); admin thì lọc theo {@code status}
     */
    public record TourSearch(String q, Long regionId, Long categoryId, Long amenityId, Long destinationId,
                             Integer minDays, Integer maxDays, Boolean featured, TourStatus status, boolean onSaleOnly) {}

    private final TourRepository tours;
    private final DepartureRepository departures;
    private final RegionRepository regions;
    private final TourCategoryRepository categories;
    private final AmenityRepository amenities;
    private final SurchargeRepository surcharges;
    private final ItineraryPlans itineraryPlans;
    private final Clock clock;

    public TourService(TourRepository tours, DepartureRepository departures, RegionRepository regions,
                       TourCategoryRepository categories, AmenityRepository amenities, SurchargeRepository surcharges,
                       ItineraryPlans itineraryPlans, Clock clock) {
        this.tours = tours;
        this.departures = departures;
        this.regions = regions;
        this.categories = categories;
        this.amenities = amenities;
        this.surcharges = surcharges;
        this.itineraryPlans = itineraryPlans;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<TourCard> search(TourSearch s, int page, int size) {
        Specification<Tour> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (s.onSaleOnly()) {
                where.add(cb.equal(root.get("status"), TourStatus.ON_SALE));
            } else if (s.status() != null) {
                where.add(cb.equal(root.get("status"), s.status()));
            }
            if (s.q() != null && !s.q().isBlank()) {
                String like = "%" + s.q().trim().toLowerCase() + "%";
                where.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("code")), like)));
            }
            if (s.regionId() != null) {
                where.add(cb.equal(root.join("regions").get("id"), s.regionId()));
            }
            if (s.categoryId() != null) {
                where.add(cb.equal(root.join("categories").get("id"), s.categoryId()));
            }
            if (s.amenityId() != null) {
                Join<Object, Object> amenity = root.join("amenities");
                where.add(cb.equal(amenity.get("amenity").get("id"), s.amenityId()));
                where.add(cb.isTrue(amenity.get("included")));
            }
            if (s.destinationId() != null) {
                // Tour có hoạt động tại điểm đến này trong lịch trình mẫu
                where.add(cb.equal(root.join("itinerary").join("days").join("activities").get("destination").get("id"), s.destinationId()));
            }
            if (s.minDays() != null) {
                where.add(cb.ge(root.get("durationDays"), s.minDays()));
            }
            if (s.maxDays() != null) {
                where.add(cb.le(root.get("durationDays"), s.maxDays()));
            }
            if (Boolean.TRUE.equals(s.featured())) {
                where.add(cb.isTrue(root.get("featured")));
            }
            query.distinct(true); // các phép nối ở trên có thể nhân bản dòng
            return cb.and(where.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50),
                Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("createdAt")));
        Page<Tour> result = tours.findAll(spec, pageable);
        Map<Long, TourPriceSummary> summaries = result.isEmpty() ? Map.of()
                : departures.summarize(result.map(Tour::getId).getContent(), DepartureStatus.OPEN, LocalDateTime.now(clock)).stream()
                .collect(Collectors.toMap(TourPriceSummary::getTourId, Function.identity()));
        return PageResponse.of(result.map(t -> TourCard.from(t, summaries.get(t.getId()))));
    }

    /** Khách chỉ xem được tour đang bán; admin xem được mọi tour. */
    @Transactional(readOnly = true)
    public TourDetail get(Long id, boolean admin) {
        Tour tour = find(id);
        if (!admin && !tour.isOnSale()) {
            throw notFound();
        }
        return TourDetail.from(tour);
    }

    public TourDetail create(TourRequest r) {
        String code = r.code().trim().toUpperCase();
        if (tours.existsByCode(code)) {
            throw codeTaken(code);
        }
        Tour tour = new Tour(code, r.name(), r.durationDays(), r.durationNights());
        apply(tour, r);
        return TourDetail.from(tours.save(tour));
    }

    public TourDetail update(Long id, TourRequest r) {
        Tour tour = find(id);
        String code = r.code().trim().toUpperCase();
        if (tours.existsByCodeAndIdNot(code, id)) {
            throw codeTaken(code);
        }
        apply(tour, r);
        return TourDetail.from(tour);
    }

    public TourDetail replaceImages(Long id, ImagesRequest r) {
        Tour tour = find(id);
        tour.replaceImages(r.images().stream()
                .map(i -> new TourImage(i.url().trim(), i.caption(), Boolean.TRUE.equals(i.cover()))).toList());
        return TourDetail.from(tour);
    }

    public TourDetail replaceItinerary(Long id, ItineraryRequest r) {
        Tour tour = find(id);
        tour.replaceItinerary(itineraryPlans.from(r));
        return TourDetail.from(tour);
    }

    /** Tour đã có đợt khởi hành thì CSDL chặn xóa (trả 409); khi đó chuyển sang ngừng bán. */
    public void delete(Long id) {
        tours.delete(find(id));
        tours.flush();
    }

    private void apply(Tour tour, TourRequest r) {
        tour.describe(r.code(), r.name(), r.summary(), r.description(), r.durationDays(), r.durationNights());
        tour.classify(
                new HashSet<>(Lookups.all(regions, r.regionIds(), "REGION_NOT_FOUND", "Có khu vực không tồn tại")),
                new HashSet<>(Lookups.all(categories, r.categoryIds(), "CATEGORY_NOT_FOUND", "Có loại hình không tồn tại")));
        List<AmenityLine> lines = r.amenities() == null ? List.of() : r.amenities();
        var amenityById = Lookups.all(amenities, lines.stream().map(AmenityLine::amenityId).toList(), "AMENITY_NOT_FOUND", "Có dịch vụ không tồn tại")
                .stream().collect(Collectors.toMap(a -> a.getId(), Function.identity()));
        tour.setAmenities(lines.stream().map(l -> new TourAmenity(amenityById.get(l.amenityId()), l.included())).toList());
        Set<Long> surchargeIds = r.surchargeIds() == null ? Set.of() : r.surchargeIds();
        tour.setSurcharges(new HashSet<>(Lookups.all(surcharges, surchargeIds, "SURCHARGE_NOT_FOUND", "Có phụ thu không tồn tại")));
        tour.setFeatured(Boolean.TRUE.equals(r.featured()));
        // Luôn gọi lại để tour đang bán được kiểm tra lại (ví dụ đổi số ngày mà lịch trình chưa đổi theo)
        tour.changeStatus(r.status() != null ? r.status() : tour.getStatus());
    }

    private Tour find(Long id) {
        return tours.findById(id).orElseThrow(TourService::notFound);
    }

    private static ApiException notFound() {
        return ApiException.notFound("NOT_FOUND", "Tour không tồn tại");
    }

    private static ApiException codeTaken(String code) {
        return ApiException.conflict("CODE_TAKEN", "Mã tour \"" + code + "\" đã tồn tại");
    }
}
