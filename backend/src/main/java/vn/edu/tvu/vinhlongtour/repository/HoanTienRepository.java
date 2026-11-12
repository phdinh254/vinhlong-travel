package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.entity.HoanTien;

import java.util.List;

public interface HoanTienRepository extends JpaRepository<HoanTien, Integer> {
    List<HoanTien> findByDonDatTour_DonDatTourIdOrderByNgayHoanAsc(Integer donId);
}
