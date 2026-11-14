package vn.edu.tvu.vinhlongtour.booking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.booking.domain.Booking;
import vn.edu.tvu.vinhlongtour.booking.domain.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn bảng đơn đặt tour.
 */
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByCode(String code);

    boolean existsByCode(String code);

    List<Booking> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    /**
     * Đọc và khóa dòng đơn tới hết giao dịch. Mọi thao tác đổi trạng thái/tiền của đơn đều khóa đơn trước,
     * rồi mới khóa đợt khởi hành (luôn cùng thứ tự để hai giao dịch không chờ nhau mãi).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    /** Mã các đơn chưa trả đồng nào và đã quá hạn giữ chỗ. */
    @Query("select b.id from Booking b where b.status = :pending and b.paidAmount = 0 and b.holdUntil < :now")
    List<Long> findOverdueHoldIds(@Param("pending") BookingStatus pending, @Param("now") LocalDateTime now);
}
