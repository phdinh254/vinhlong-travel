package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.entity.YeuCauHuy;

import java.util.List;

public interface YeuCauHuyRepository extends JpaRepository<YeuCauHuy, Integer> {
    List<YeuCauHuy> findByDonDatTour_DonDatTourIdOrderByThoiGianYeuCauDesc(Integer donId);
    List<YeuCauHuy> findAllByOrderByThoiGianYeuCauDesc();
    boolean existsByDonDatTour_DonDatTourIdAndKetQua(Integer donId, String ketQua);
}
