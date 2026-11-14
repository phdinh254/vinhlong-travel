package vn.edu.tvu.vinhlongtour.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.auth.domain.Role;

import java.util.Optional;

/**
 * Truy vấn bảng {@code accounts}. Email truyền vào phải đã chuẩn hóa
 * bằng {@link Account#normalizeEmail(String)}.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);

    Optional<Account> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    Page<Account> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(String email, String fullName, Pageable pageable);
}
