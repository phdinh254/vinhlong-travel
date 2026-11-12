package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.edu.tvu.vinhlongtour.entity.Tour;

public interface TourRepository extends JpaRepository<Tour, Integer>, JpaSpecificationExecutor<Tour> {
    boolean existsByMaTourIgnoreCase(String maTour);
}
