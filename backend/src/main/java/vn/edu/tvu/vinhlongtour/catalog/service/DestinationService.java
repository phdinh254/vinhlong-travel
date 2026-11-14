package vn.edu.tvu.vinhlongtour.catalog.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;
import vn.edu.tvu.vinhlongtour.catalog.domain.GeoPoint;
import vn.edu.tvu.vinhlongtour.catalog.domain.Region;
import vn.edu.tvu.vinhlongtour.catalog.domain.TimeWindow;
import vn.edu.tvu.vinhlongtour.catalog.domain.TourCategory;
import vn.edu.tvu.vinhlongtour.catalog.repository.DestinationRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.RegionRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.TourCategoryRepository;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.DestinationRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.DestinationResponse;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.PageResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nghiệp vụ điểm đến: khách xem và lọc các điểm đang hiển thị; admin thêm, sửa, ẩn, xóa.
 */
@Service
@Transactional
public class DestinationService {

    private final DestinationRepository destinations;
    private final RegionRepository regions;
    private final TourCategoryRepository categories;

    public DestinationService(DestinationRepository destinations, RegionRepository regions, TourCategoryRepository categories) {
        this.destinations = destinations;
        this.regions = regions;
        this.categories = categories;
    }

    /**
     * Tìm điểm đến theo khu vực, loại hình và từ khóa trong tên (bỏ trống thì không lọc theo điều kiện đó).
     *
     * @param includeHidden true khi admin xem, để thấy cả điểm đã ẩn
     */
    @Transactional(readOnly = true)
    public PageResponse<DestinationResponse> search(Long regionId, Long categoryId, String q, boolean includeHidden, int page, int size) {
        Specification<Destination> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (!includeHidden) {
                conditions.add(cb.isTrue(root.get("active")));
            }
            if (regionId != null) {
                conditions.add(cb.equal(root.get("region").get("id"), regionId));
            }
            if (categoryId != null) {
                conditions.add(cb.equal(root.join("categories").get("id"), categoryId));
            }
            if (q != null && !q.isBlank()) {
                conditions.add(cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase() + "%"));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("code"));
        return PageResponse.of(destinations.findAll(spec, pageable).map(DestinationResponse::from));
    }

    /** Khách chỉ xem được điểm đang hiển thị; admin xem được cả điểm đã ẩn. */
    @Transactional(readOnly = true)
    public DestinationResponse get(Long id, boolean includeHidden) {
        Destination d = find(id);
        if (!d.isActive() && !includeHidden) {
            throw notFound();
        }
        return DestinationResponse.from(d);
    }

    public DestinationResponse create(DestinationRequest r) {
        if (destinations.existsByCode(r.code().trim().toUpperCase())) {
            throw codeTaken(r.code());
        }
        Destination d = new Destination(r.code(), r.name(), region(r.regionId()));
        apply(d, r);
        return DestinationResponse.from(destinations.save(d));
    }

    public DestinationResponse update(Long id, DestinationRequest r) {
        Destination d = find(id);
        if (destinations.existsByCodeAndIdNot(r.code().trim().toUpperCase(), id)) {
            throw codeTaken(r.code());
        }
        d.rename(r.code(), r.name());
        apply(d, r);
        return DestinationResponse.from(d);
    }

    public void delete(Long id) {
        destinations.delete(find(id));
        destinations.flush();
    }

    private void apply(Destination d, DestinationRequest r) {
        d.locate(region(r.regionId()), r.address(), new GeoPoint(r.latitude(), r.longitude()), r.coordinateQuality());
        d.classify(categories(r.categoryIds()));
        d.schedule(new TimeWindow(r.openTime(), r.closeTime()), r.closedDays() == null ? Set.of() : r.closedDays(), r.hoursQuality());
        d.price(r.ticketPriceAdult(), r.ticketPriceChild(), r.priceQuality());
        d.rate(r.visitMinutes(), r.popularity());
        d.describe(r.description(), r.sources());
        d.setActive(r.active() == null || r.active());
    }

    private Region region(Long id) {
        return regions.findById(id).orElseThrow(() -> ApiException.badRequest("REGION_NOT_FOUND", "Khu vực không tồn tại"));
    }

    private Set<TourCategory> categories(Set<Long> ids) {
        List<TourCategory> found = categories.findAllById(ids);
        if (found.size() != ids.size()) {
            throw ApiException.badRequest("CATEGORY_NOT_FOUND", "Có loại hình không tồn tại");
        }
        return new HashSet<>(found);
    }

    private Destination find(Long id) {
        return destinations.findById(id).orElseThrow(DestinationService::notFound);
    }

    private static ApiException notFound() {
        return ApiException.notFound("NOT_FOUND", "Điểm đến không tồn tại");
    }

    private static ApiException codeTaken(String code) {
        return ApiException.conflict("CODE_TAKEN", "Mã điểm đến \"" + code.trim().toUpperCase() + "\" đã tồn tại");
    }
}
