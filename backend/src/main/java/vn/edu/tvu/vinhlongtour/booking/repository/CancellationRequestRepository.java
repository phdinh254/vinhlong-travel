package vn.edu.tvu.vinhlongtour.booking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.booking.domain.CancellationRequest;
import vn.edu.tvu.vinhlongtour.booking.domain.CancellationRequestStatus;

import java.util.Optional;

/**
 * Truy vấn bảng yêu cầu hủy (để admin xem danh sách chờ duyệt).
 */
public interface CancellationRequestRepository extends JpaRepository<CancellationRequest, Long> {

    Page<CancellationRequest> findByStatus(CancellationRequestStatus status, Pageable pageable);

    /** Chỉ lấy mã đơn để khóa đơn trước, rồi mới đọc yêu cầu qua đơn. */
    @Query("select r.booking.id from CancellationRequest r where r.id = :id")
    Optional<Long> findBookingId(@Param("id") Long id);
}
