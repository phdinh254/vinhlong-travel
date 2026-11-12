package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.entity.PhuThu;

import java.util.List;

public interface PhuThuRepository extends JpaRepository<PhuThu, Integer> {
    /** Phụ thu áp dụng cho một tour = phụ thu riêng của tour + phụ thu chung */
    @Query("select p from PhuThu p left join p.tour t where p.conHieuLuc = true and (t is null or t.tourId = :tourId)")
    List<PhuThu> findApDungChoTour(@Param("tourId") Integer tourId);
}
