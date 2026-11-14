package vn.edu.tvu.vinhlongtour.catalog.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.tvu.vinhlongtour.catalog.domain.Amenity;
import vn.edu.tvu.vinhlongtour.catalog.domain.Region;
import vn.edu.tvu.vinhlongtour.catalog.domain.TourCategory;
import vn.edu.tvu.vinhlongtour.catalog.repository.AmenityRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.RegionRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.TourCategoryRepository;

/**
 * Tạo ba service cho ba danh mục chỉ có tên, dùng chung class {@link NamedItemService}.
 */
@Configuration
class CatalogConfig {

    @Bean
    NamedItemService<Region> regionService(RegionRepository repo) {
        return new NamedItemService<>("regions", "Khu vực", repo, Region::new);
    }

    @Bean
    NamedItemService<TourCategory> tourCategoryService(TourCategoryRepository repo) {
        return new NamedItemService<>("tour-categories", "Loại hình", repo, TourCategory::new);
    }

    @Bean
    NamedItemService<Amenity> amenityService(AmenityRepository repo) {
        return new NamedItemService<>("amenities", "Dịch vụ", repo, Amenity::new);
    }
}
