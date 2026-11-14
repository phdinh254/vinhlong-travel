package vn.edu.tvu.vinhlongtour.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.planner.domain.SavedTrip;

import java.util.List;
import java.util.Optional;

/**
 * Truy vấn bảng lịch trình đã lưu ({@code saved_trips}); khách chỉ thấy lịch của chính mình.
 */
public interface SavedTripRepository extends JpaRepository<SavedTrip, Long> {

    List<SavedTrip> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    Optional<SavedTrip> findByIdAndAccountId(Long id, Long accountId);
}
