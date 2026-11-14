package vn.edu.tvu.vinhlongtour.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;

import java.util.List;

/**
 * Truy vấn bảng điểm đến ({@code destinations}); lọc động theo khu vực, loại hình, từ khóa
 * qua {@link JpaSpecificationExecutor}.
 */
public interface DestinationRepository extends JpaRepository<Destination, Long>, JpaSpecificationExecutor<Destination> {

    boolean existsByCode(String code);

    List<Destination> findByActiveTrue();

    boolean existsByCodeAndIdNot(String code, Long id);
}
