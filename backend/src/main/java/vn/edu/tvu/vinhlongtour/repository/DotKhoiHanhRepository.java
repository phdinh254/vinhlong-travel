package vn.edu.tvu.vinhlongtour.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.entity.DotKhoiHanh;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DotKhoiHanhRepository extends JpaRepository<DotKhoiHanh, Integer> {

    List<DotKhoiHanh> findByTour_TourIdOrderByNgayGioDiAsc(Integer tourId);

    @Query("""
            select d from DotKhoiHanh d
            where d.tour.tourId = :tourId and d.trangThai in ('MO_BAN','HET_CHO')
              and d.hanNhanKhach > :now
            order by d.ngayGioDi
            """)
    List<DotKhoiHanh> findDangMoBan(@Param("tourId") Integer tourId, @Param("now") LocalDateTime now);

    /** Khóa dòng khi đặt chỗ để tránh bán vượt sức chứa (Q15, Q19, Q31) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DotKhoiHanh d where d.dotKhoiHanhId = :id")
    Optional<DotKhoiHanh> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
            select min(b.donGia) from BangGiaDot b
            where b.dotKhoiHanh.tour.tourId = :tourId and b.donGia > 0
              and b.dotKhoiHanh.trangThai = 'MO_BAN' and b.dotKhoiHanh.hanNhanKhach > :now
            """)
    BigDecimal findGiaThapNhat(@Param("tourId") Integer tourId, @Param("now") LocalDateTime now);

    boolean existsByMaDotIgnoreCase(String maDot);
}
