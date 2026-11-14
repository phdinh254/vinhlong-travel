package vn.edu.tvu.vinhlongtour.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.catalog.domain.CancellationPolicy;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Truy vấn bảng chính sách hủy ({@code cancellation_policies}).
 */
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {

    /** Chính sách áp dụng vào ngày {@code date}: chính sách có ngày hiệu lực gần nhất mà không sau ngày đó. */
    Optional<CancellationPolicy> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(LocalDate date);
}
