package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.entity.LichSuChuyenDot;

import java.util.List;

public interface LichSuChuyenDotRepository extends JpaRepository<LichSuChuyenDot, Integer> {
    List<LichSuChuyenDot> findByDonDatTour_DonDatTourIdOrderByThoiGianAsc(Integer donId);
}
