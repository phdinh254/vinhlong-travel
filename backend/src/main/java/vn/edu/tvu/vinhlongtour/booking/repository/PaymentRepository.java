package vn.edu.tvu.vinhlongtour.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.booking.domain.Payment;

import java.util.Optional;

/**
 * Truy vấn bảng thanh toán.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(String reference);

    /** Chỉ lấy mã đơn (chưa nạp thanh toán) để khóa đơn trước, rồi mới đọc thanh toán. */
    @Query("select p.booking.id from Payment p where p.reference = :reference")
    Optional<Long> findBookingIdByReference(@Param("reference") String reference);
}
