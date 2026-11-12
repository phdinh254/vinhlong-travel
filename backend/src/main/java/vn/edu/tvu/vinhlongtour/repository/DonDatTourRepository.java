package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.entity.DonDatTour;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DonDatTourRepository extends JpaRepository<DonDatTour, Integer>, JpaSpecificationExecutor<DonDatTour> {
    Optional<DonDatTour> findByMaDon(String maDon);

    List<DonDatTour> findByKhachHang_KhachHangIdOrderByNgayDatDesc(Integer khachHangId);

    /** Q31: đơn quá hạn giữ chỗ mà chưa thanh toán đồng nào */
    @Query("""
            select d.donDatTourId from DonDatTour d
            where d.trangThai = 'CHO_THANH_TOAN' and d.hanGiuCho < :now and d.daThanhToan = 0
            """)
    List<Integer> findIdsHetHanGiuCho(@Param("now") LocalDateTime now);

    long countByTrangThai(String trangThai);

    @Query("select coalesce(sum(d.daThanhToan),0) from DonDatTour d")
    BigDecimal tongDoanhThuDaThu();
}
