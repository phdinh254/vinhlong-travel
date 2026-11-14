package vn.edu.tvu.vinhlongtour.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.edu.tvu.vinhlongtour.tour.domain.Tour;

/**
 * Truy vấn bảng tour; lọc động (khu vực, loại hình, dịch vụ, điểm đến, số ngày...) qua {@link JpaSpecificationExecutor}.
 */
public interface TourRepository extends JpaRepository<Tour, Long>, JpaSpecificationExecutor<Tour> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
