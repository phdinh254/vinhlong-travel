package vn.edu.tvu.vinhlongtour.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.DepartureStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Truy vấn bảng đợt khởi hành.
 */
public interface DepartureRepository extends JpaRepository<Departure, Long> {

    List<Departure> findByTourIdOrderByDepartureAt(Long tourId);

    /** Các đợt khách còn xem được của một tour: đang mở và chưa quá hạn nhận khách (kể cả đợt đã hết chỗ). */
    List<Departure> findByTourIdAndStatusAndBookingDeadlineAfterOrderByDepartureAt(Long tourId, DepartureStatus status, LocalDateTime now);

    boolean existsByCode(String code);

    /**
     * "Giá từ" và ngày đi gần nhất của nhiều tour trong một câu truy vấn (tránh mỗi tour một truy vấn).
     * Chỉ tính đợt đang mở, chưa quá hạn, còn chỗ; giá lấy của loại khách người lớn
     * (loại có tuổi bắt đầu lớn nhất).
     */
    @Query("""
            select d.tour.id as tourId, min(p.price) as fromPrice, min(d.departureAt) as nextDepartureAt
            from Departure d join d.prices p
            where d.tour.id in :tourIds
              and d.status = :open
              and d.bookingDeadline > :now
              and d.bookedSeats < d.capacity
              and p.passengerType.ageRange.minAge = (select max(t.ageRange.minAge) from PassengerType t)
            group by d.tour.id
            """)
    List<TourPriceSummary> summarize(@Param("tourIds") Collection<Long> tourIds,
                                     @Param("open") DepartureStatus open,
                                     @Param("now") LocalDateTime now);

    /** Một dòng kết quả của {@link #summarize}. */
    interface TourPriceSummary {
        Long getTourId();

        Long getFromPrice();

        LocalDateTime getNextDepartureAt();
    }
}
