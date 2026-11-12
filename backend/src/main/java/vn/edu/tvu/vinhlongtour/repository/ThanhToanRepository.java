package vn.edu.tvu.vinhlongtour.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.entity.ThanhToan;

import java.util.List;
import java.util.Optional;

public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ThanhToan t where t.maThamChieu = :ref")
    Optional<ThanhToan> findByMaThamChieuForUpdate(@Param("ref") String maThamChieu);

    List<ThanhToan> findByDonDatTour_DonDatTourIdOrderByNgayTaoAsc(Integer donId);
}
