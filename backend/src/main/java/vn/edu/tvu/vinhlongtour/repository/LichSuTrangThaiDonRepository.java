package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.entity.LichSuTrangThaiDon;

import java.util.List;

public interface LichSuTrangThaiDonRepository extends JpaRepository<LichSuTrangThaiDon, Integer> {
    List<LichSuTrangThaiDon> findByDonDatTour_DonDatTourIdOrderByThoiGianAsc(Integer donId);
}
