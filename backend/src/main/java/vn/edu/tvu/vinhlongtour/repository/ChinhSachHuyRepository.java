package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.entity.ChinhSachHuy;

import java.time.LocalDate;
import java.util.List;

public interface ChinhSachHuyRepository extends JpaRepository<ChinhSachHuy, Integer> {
    /** Ứng viên đang hiệu lực: chính sách riêng của tour xếp trước, sau đó phiên bản mới nhất */
    @Query("""
            select c from ChinhSachHuy c left join c.tour t
            where (t is null or t.tourId = :tourId)
              and c.tuNgay <= :ngay and (c.denNgay is null or c.denNgay >= :ngay)
            order by case when t is null then 1 else 0 end, c.phienBan desc
            """)
    List<ChinhSachHuy> findHieuLuc(@Param("tourId") Integer tourId, @Param("ngay") LocalDate ngay);
}
